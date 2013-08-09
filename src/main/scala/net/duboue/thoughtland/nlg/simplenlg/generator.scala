/*
 *   This file is part of Thoughtland -- Verbalizing n-dimensional objects.
 *   Copyright (C) 2013 Pablo Duboue <pablo.duboue@gmail.com>
 * 
 *   Thoughtland is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as 
 *   published by the Free Software Foundation, either version 3 of 
 *   the License, or (at your option) any later version.
 *
 *   Meetdle is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *   
 *   You should have received a copy of the GNU Affero General Public 
 *   License along with Thoughtland.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.duboue.thoughtland.nlg.simplenlg

import java.util.Locale

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.collectionAsScalaIterable

import org.jgrapht.alg.BronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import org.xml.sax.InputSource

import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Finding
import net.duboue.thoughtland.GeneratedText
import net.duboue.thoughtland.Generator
import net.duboue.thoughtland.Paragraph
import net.duboue.thoughtland.RelativeMagnitude
import net.duboue.thoughtland.RelativeMagnitude.valueToRelativeMagnitude
import net.duboue.thoughtland.Sentence
import net.duboue.thoughtland.nlg.BasicVerbalizations
import net.sf.openschema.Frame
import net.sf.openschema.FrameSet
import net.sf.openschema.GreedyChooser
import net.sf.openschema.OpenSchemaPlanner
import net.sf.openschema.RDFOntology
import net.sf.openschema.SimpleFocusChooser
import net.sf.openschema.util.SchemaToXmlFilterStream
import simplenlg.features.Feature
import simplenlg.features.NumberAgreement
import simplenlg.framework.CoordinatedPhraseElement
import simplenlg.framework.NLGFactory
import simplenlg.lexicon.Lexicon
import simplenlg.phrasespec.NPPhraseSpec
import simplenlg.phrasespec.SPhraseSpec
import simplenlg.realiser.english.Realiser

class SimpleNlgGenerator extends Generator with AnalysisAsFrames with BasicVerbalizations with DocumentPlansAsThoughtlandPlans {

  val ontology = new RDFOntology(classOf[SimpleNlgGenerator].getResourceAsStream("ontology.rdfs"),
    "classpath://net/duboue/thoughtland/nlg/simplenlg/ontology.rdfs");

  val byComponentSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-component.schema"))), new SimpleFocusChooser(ontology));
  val byAttributeSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-attribute.schema"))), new GreedyChooser());

  val lexicon = Lexicon.getDefaultLexicon();
  val nlgFactory = new NLGFactory(lexicon);
  val realiser = new Realiser(lexicon);

  def apply(analysis: Analysis)(implicit env: Environment): GeneratedText = {
    System.out.println(analysis)
    val frames = analysisToFrameSet(analysis, env.config.useFunkyNames);
    val texts = List(byComponentSchema, byAttributeSchema)
      .map { _.instantiate(frames, new java.util.HashMap(), ontology) }
      .map { asThoughtlandPlan(_, frames) }
      .map(verbalize(frames, _));
    if (texts(0).toString.length < texts(1).toString.length)
      return texts(0)
    else
      return texts(1)
  }

  /**
   * Given the component and the findings, produce a string name for it (not necessarily unique).
   */
  def chooseComponentName(component: Int, findings: List[Finding], existingNames: Set[String],
    rnd: java.util.Random, useFunkyNames: Boolean): String = {
    val size = findings.filter({ f =>
      f match {
        case ComponentSize(c, _) => c == component;
        case _ => false
      }
    }).headOption
    val density = findings.filter({ f =>
      f match {
        case ComponentDensity(c, _) => c == component;
        case _ => false
      }
    }).headOption

    def titleCase(s: String) = s(0).toUpper + s.substring(1).toLowerCase(Locale.ENGLISH)

    val potentialNames = if (useFunkyNames && size.isDefined && density.isDefined)
      densityBasedNames(density.get.asInstanceOf[ComponentDensity].density)
    else if (useFunkyNames && size.isDefined)
      sizeBasedNames(size.get.asInstanceOf[ComponentSize].size)
    else if (useFunkyNames && density.isDefined)
      densityBasedNames(density.get.asInstanceOf[ComponentDensity].density)
    else
      List(component.toString) //numToStr(component))
    val finalNames =
      if (potentialNames.toSet.subsetOf(existingNames))
        (existingNames -- potentialNames).toList
      else
        potentialNames // name will be repeated

    val baseName = titleCase(finalNames(rnd.nextInt(finalNames.length)))
    var suffixCounter = 1;
    var potentialName = baseName
    while (existingNames.contains(potentialName)) {
      suffixCounter += 1;
      potentialName = s"$baseName$suffixCounter"
    }
    potentialName
  }

  def verbalize(frames: FrameSet, plan: ThoughtlandPlan): GeneratedText = {
    // helper functions
    def componentNamesToNP(names: List[String]): NPPhraseSpec = names match {
      case List(name) => nlgFactory.createNounPhrase(s"component $name")
      case _ => {
        val np = nlgFactory.createNounPhrase("component")
        val c = nlgFactory.createCoordinatedPhrase()
        names.map { nlgFactory.createNounPhrase(_) }.foreach { c.addCoordinate(_) }
        np.addPostModifier(c)
        np
      }
    }

    def typeStrToMagnitude(s: String) = RelativeMagnitude.values.filter(v => v.typeStr.equals(s)).head
    def verbalizeMagnitude(m: RelativeMagnitude.RelativeMagnitude)(implicit _type: String) = _type match {
      case "c-size" => sizeToStr(m)
      case "c-density" => densityToStr(m)
      case "c-distance" => distanceToStr(m)
    }
    // template system for fall-back
    def verbalize(obj: Object): String =
      obj match {
        case s: String => if (s.startsWith("\"")) s.substring(1, s.length() - 1) else s
        case m: java.util.Map[String @unchecked, Object @unchecked] => {
          val fd = frames.getFrame(m.get("object-id").toString)
          if (ontology.isA(fd.getType(), "c-distance")) {
            val components = fd.get("component")
            val component1 = components(0).asInstanceOf[Frame]
            val component2 = components(1).asInstanceOf[Frame]
            s"${component1.get("name").head} and ${component2.get("name").head}"
          } else if (fd.containsKey("name")) {
            fd.get("name").head.toString
          } else {
            fd.toString
          }
        }
        case x => x.toString() + " [" + x.getClass() + "]"
      }
    def templateClauses(clauses: List[PlanClause]): List[Sentence] =
      clauses.filter { _.hasTemplate }.map(_.templateClause(verbalize)).toList

    // generate
    new GeneratedText(plan.paras.map { para =>
      Paragraph(para.aggr.map { aggrSegment =>
        // this is like a 'where' clause in Haskell. This is the return value, 'sentences'
        var sentences: List[Sentence] = null; // using var to avoid forward reference error
        if (aggrSegment.clauses.length == 1 && aggrSegment.clauses(0).getString("pred").equals("c-conjunction"))
          sentences = generateIntroductorySentence()
        else if (aggrSegment.clauses.forall(clause => clause.getString("pred").equals("has-attribute")))
          // all entries in aggregation set have the same attribute
          sentences = generateAttributeAggregatedSentences()
        else if (aggrSegment.clauses.map(clause => clause.getString("pred0"))
          .foldLeft((null: String, true))((p, s) => if (p._2) (if (p._1 == null) (s, true) else (s, p._1.equals(s)))
          else p)._2)
          // else, all about the same component
          // if all entries talk about the same entity, glue them together but keep an eye to mix with the next one
          sentences = generateComponentAggregatedSentences()
        else
          // unknown, go template route
          sentences = templateClauses(aggrSegment.clauses.toList);

        def generateIntroductorySentence() = {
          val fd = aggrSegment.clauses(0)
          val p = nlgFactory.createClause();
          p.setSubject("there");
          p.getSubject().setFeature("expletive_subject", false);
          p.setVerb("be");
          val phrCom = nlgFactory.createNounPhrase("component");
          val numCom = Integer.parseInt(fd.getVariable2("0", "pred1"));
          phrCom.setSpecifier(numToStr(numCom));
          if (numCom > 1) {
            p.getSubject().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
            phrCom.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
          }
          val phrDim = nlgFactory.createNounPhrase("dimension");
          val numDim = Integer.parseInt(fd.getVariable2("1", "pred1"));
          phrDim.setSpecifier(numToStr(numDim));
          phrDim.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);

          p.setObject(new CoordinatedPhraseElement(phrCom, phrDim));
          //              realiser.setDebugMode(true)
          List(Sentence(realiser.realiseSentence(p)))
        }

        def generateAttributeAggregatedSentences() = {
          implicit val _type = aggrSegment.clauses(0).getFrame("pred1").getType.toString
          var attributeSentences: List[Sentence] = null;

          if (_type.equals("c-distance"))
            attributeSentences = generateDistanceAggregatedSentences()
          else
            attributeSentences = generateNonDistanceAttributeAggregatedSentences();

          def generateDistanceAggregatedSentences() = {
            // distances are a special sub-case, two strategies: 
            // (1) order by distance (components one, two and three are far from each other)
            // (2) order by component (component one is near all the other, 
            //     component two is close to component three)

            // first filter repeated pairs
            val seen = new scala.collection.mutable.HashSet[String] // pair of component names, sorted
            val filtered = aggrSegment.clauses.filter {
              clause =>
                val key = clause.getFrame("pred1").get("component").map {
                  _.asInstanceOf[Frame].get("name").head.toString
                }.sorted.mkString("-")
                if (seen.contains(key))
                  false
                else {
                  // side effects are bad for you, don't try this at home
                  seen += key
                  true
                }
            }
            // sort by distance
            val pairsAtADistance = new scala.collection.mutable.HashMap[String, scala.collection.mutable.Buffer[Pair[String, String]]]
            filtered.foreach {
              clause =>
                val components = clause.getFrame("pred1").get("component").map {
                  _.asInstanceOf[Frame].get("name").head.toString
                }.sorted
                val distance = clause.getVariable("pred2")
                if (!pairsAtADistance.contains(distance))
                  pairsAtADistance += distance -> new scala.collection.mutable.ArrayBuffer[Pair[String, String]]
                pairsAtADistance(distance) += Pair(components(0), components(1))
            }

            // see which distance is most popular and skip it
            val mostPopular = pairsAtADistance.map { p => Pair(p._1, p._2.size) }./:("", 0)((best, current) =>
              if (current._2 > best._2) current else best)
            val hasMostPopular = mostPopular._2 > 0
            if (hasMostPopular)
              pairsAtADistance -= mostPopular._1

            // see if there are any full cliques at a given distance
            val cliquesAtADistance = pairsAtADistance.map {
              x =>
                x match {
                  case (distance, pairs) =>
                    val graph = new SimpleGraph[String, DefaultEdge](classOf[DefaultEdge])
                    pairs.foreach { p =>
                      p match {
                        case (c1, c2) =>
                          graph.addVertex(c1);
                          graph.addVertex(c2);
                          graph.addEdge(c1, c2)
                      }
                    }
                    // look for maximal cliques
                    val cliques = new BronKerboschCliqueFinder(graph).getAllMaximalCliques().map { s =>
                      s.toList.sorted
                    }.toList.filter(_.size > 2)

                    // remove clique pairs from pairsAtADistance
                    cliques.foreach {
                      cliqueNodes =>
                        for (i <- 0.to(cliqueNodes.size - 1))
                          for (j <- (i + 1).to(cliqueNodes.size - 1))
                            pairs -= Pair(cliqueNodes(i), cliqueNodes(j))
                    }
                    pairsAtADistance(distance) = pairs

                    Tuple2(distance, cliques)
                }
            }

            // verbalize them
            val cliqueSentences: List[SPhraseSpec] = cliquesAtADistance.map {
              p =>
                p match {
                  case (distance, cliques) =>
                    var first = true
                    cliques.map {
                      clique =>
                        val clause = nlgFactory.createClause(componentNamesToNP(clique), "be", "all")
                        if (!first)
                          clause.addComplement(nlgFactory.createAdverbPhrase("also"))
                        else
                          first = false
                        clause.addComplement(verbalizeMagnitude(typeStrToMagnitude(distance)))
                        clause.addComplement("each other")

                        clause
                    }
                }
            }.toList.flatten

            // verbalize the rest
            //TODO order by component and find similarities

            val restSentences: List[SPhraseSpec] = pairsAtADistance.map {
              x =>
                x match {
                  case (distance, pairs) =>
                    var first = true
                    pairs.map {
                      pair =>
                        val clause = nlgFactory.createClause(componentNamesToNP(List(pair._1, pair._2)), "be")
                        if (!first)
                          clause.addComplement(nlgFactory.createAdverbPhrase("also"))
                        else
                          first = false
                        clause.addComplement(verbalizeMagnitude(typeStrToMagnitude(distance)))
                        clause.addComplement("each other")

                        clause
                    }
                }
            }.toList.flatten

            val mostPopularPhrase: List[SPhraseSpec] = if (hasMostPopular) {
              val clause = nlgFactory.createClause("the rest", "be", "all");
              clause.getSubject().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
              clause.setVerb("be");
              clause.addComplement(verbalizeMagnitude(typeStrToMagnitude(mostPopular._1)))
              clause.addComplement("each other")
              List(clause)
            } else
              List[SPhraseSpec]()

            (cliqueSentences ++ restSentences ++ mostPopularPhrase).map { realiser.realiseSentence(_) }.map { Sentence(_) }
            //templateClauses(sorted.toList)
          }

          def generateNonDistanceAttributeAggregatedSentences() = {
            // order them by magnitude value
            val sorted = aggrSegment.clauses.sortBy[String](clause => clause.getVariable("pred2"));
            val c = nlgFactory.createCoordinatedPhrase();
            var current: Pair[List[String], RelativeMagnitude.RelativeMagnitude] = null;
            // helper function, adds current component / magnitude to the coordinated phrase
            def addToPhrase() = if (current != null) {
              c.addCoordinate(
                nlgFactory.createClause(componentNamesToNP(current._1), "be", verbalizeMagnitude(current._2)))
            }
            sorted.foreach {
              clause =>
                val name = clause.getFrame("pred0").get("name").head.toString
                val magnitude = typeStrToMagnitude(clause.getVariable("pred2"))
                if (current == null)
                  current = Pair(List(name), magnitude)
                else if (magnitude == current._2)
                  current = Pair(current._1 ++ List(name), current._2)
                else {
                  addToPhrase
                  current = Pair(List(name), magnitude)
                }
            }
            addToPhrase()

            List(Sentence(realiser.realiseSentence(c)))
          }

          attributeSentences
        }

        def generateComponentAggregatedSentences() = {
          // component 1 is dense, big and at a good distance from components 2 and 3. It is far from component 4.
          if (aggrSegment.clauses.length > 2) { // skip over "there is a component 1"

            val name = aggrSegment.clauses(0).getFrame("pred0").get("name").head.toString

            // Have a fixed order to search for information on the frames
            //TODO order properly, density, size, distance
            val attributes = aggrSegment.clauses
              .filter(clause =>
                clause.getFrame("pred1") != null &&
                  clause.getFrame("pred1").getType != null &&
                  !clause.getFrame("pred1").getType.toString.equals("c-distance"))
              .sortBy[String](clause => clause.getFrame("pred1").getType().toString());
            val distances = aggrSegment.clauses
              .filter(clause =>
                clause.getFrame("pred1") != null &&
                  clause.getFrame("pred1").getType != null &&
                  clause.getFrame("pred1").getType.toString.equals("c-distance"))
              .sortBy[String](clause => clause.getFrame("pred0").get("name").head.toString);

            var currentCoord = nlgFactory.createCoordinatedPhrase()
            var currentPhrase: SPhraseSpec = nlgFactory.createClause(componentNamesToNP(List(name)), "be", currentCoord)
            var emptyPhrase = true
            val toRealise = new scala.collection.mutable.ArrayBuffer[SPhraseSpec]
            attributes.foreach {
              clause =>
                val _type = clause.getFrame("pred1").getType.toString
                val magnitude = typeStrToMagnitude(clause.getVariable("pred2"))
                currentCoord.addCoordinate(nlgFactory.createNounPhrase(verbalizeMagnitude(magnitude)(_type)))
                emptyPhrase = false
            }
            if (!distances.isEmpty) {
              // distance subsystem
              // TODO unify both distance systems

              // count for each magnitude, see if there's a most popular phrase
              val atADistance = new scala.collection.mutable.HashMap[String, scala.collection.mutable.Buffer[String]]
              distances.foreach {
                clause =>
                  val distance = clause.getVariable("pred2")
                  val other = clause.getFrame("pred1").get("component").map {
                    _.asInstanceOf[Frame].get("name").head.toString
                  }.filter(n => !n.equals(name)).head
                  if (!atADistance.contains(distance))
                    atADistance += distance -> new scala.collection.mutable.ArrayBuffer[String]
                  atADistance(distance) += other
              }

              val mostPopular = atADistance.map { p => Pair(p._1, p._2.size) }./:("", 0)((best, current) =>
                if (current._2 > best._2) current else best)
              val hasMostPopular = mostPopular._2 > 0
              if (hasMostPopular)
                atADistance -= mostPopular._1

              atADistance.foreach {
                x =>
                  x match {
                    case (distance, others) =>
                      val outputNow = !emptyPhrase
                      val also = !toRealise.isEmpty

                      //TODO this is a template
                      currentCoord.addCoordinate(verbalizeMagnitude(typeStrToMagnitude(distance))("c-distance") + " " +
                        realiser.realise(componentNamesToNP(others.toList)))
                      emptyPhrase = false

                      if (also)
                        currentPhrase.addComplement(nlgFactory.createAdverbPhrase("also"))

                      if (outputNow) {
                        toRealise += currentPhrase
                        currentCoord = nlgFactory.createCoordinatedPhrase()
                        currentPhrase = nlgFactory.createClause("it", "be", currentCoord)
                        emptyPhrase = true
                      }
                  }
              }

              if (hasMostPopular) {
                if (!emptyPhrase) {
                  toRealise += currentPhrase

                  currentCoord = nlgFactory.createCoordinatedPhrase()
                  currentPhrase = nlgFactory.createClause("it", "be", currentCoord)
                  emptyPhrase = true
                }
                val all = atADistance.isEmpty
                val clause = if (all) nlgFactory.createClause(
                  "all components",
                  "be")
                else nlgFactory.createClause(
                  "the rest",
                  "be", "all");
                clause.getSubject().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
                clause.setVerb("be");
                //TODO this is a template
                clause.addComplement(verbalizeMagnitude(typeStrToMagnitude(mostPopular._1))("c-distance") + " " +
                  (if (toRealise.isEmpty)
                    realiser.realise(componentNamesToNP(List(name)))
                  else
                    "it"))
                toRealise += clause
              }
            }
            if (!emptyPhrase) {
              toRealise += currentPhrase
            }
            toRealise.toList.map { realiser.realiseSentence(_) }.map { Sentence(_) }
          } else
            List()
        }

        sentences
      }./:(List[Sentence]())(_ ++ _))
    }.toList)
  }
}

