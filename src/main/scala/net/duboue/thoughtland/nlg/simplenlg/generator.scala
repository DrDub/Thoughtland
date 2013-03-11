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

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import org.xml.sax.InputSource
import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.GeneratedText
import net.duboue.thoughtland.Generator
import net.duboue.thoughtland.Paragraph
import net.duboue.thoughtland.RelativeMagnitude
import net.duboue.thoughtland.RelativeMagnitude.valueToRelativeMagnitude
import net.duboue.thoughtland.Sentence
import net.duboue.thoughtland.nlg.BasicVerbalizations
import net.sf.openschema.DocumentPlan
import net.sf.openschema.Frame
import net.sf.openschema.FrameSet
import net.sf.openschema.GreedyChooser
import net.sf.openschema.OpenSchemaPlanner
import net.sf.openschema.RDFOntology
import net.sf.openschema.SimpleFocusChooser
import net.sf.openschema.util.SchemaToXmlFilterStream
import simplenlg.features.Feature
import simplenlg.features.NumberAgreement
import simplenlg.framework.NLGFactory
import simplenlg.lexicon.Lexicon
import simplenlg.phrasespec.NPPhraseSpec
import simplenlg.realiser.english.Realiser
import simplenlg.framework.CoordinatedPhraseElement

class SimpleNlgGenerator extends Generator with AnalysisAsFrames with BasicVerbalizations {

  val ontology = new RDFOntology(classOf[SimpleNlgGenerator].getResourceAsStream("ontology.rdfs"),
    "classpath://net/duboue/thoughtland/nlg/simplenlg/ontology.rdfs");

  val byComponentSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-component.schema"))), new SimpleFocusChooser(ontology));
  val byAttributeSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-attribute.schema"))), new GreedyChooser());

  val lexicon = Lexicon.getDefaultLexicon();
  val nlgFactory = new NLGFactory(lexicon);
  val realiser = new Realiser(lexicon);

  def apply(analysis: Analysis)(implicit env: Environment): GeneratedText = {
    val frames = analysisToFrameSet(analysis);
    val texts = List(byComponentSchema, byAttributeSchema)
      .map { _.instantiate(frames, new java.util.HashMap(), ontology) }
      .map(verbalize(frames, _));
    if (false) // TODO texts(0).toString.length < texts(1).toString.length)
      return texts(0)
    else
      return texts(1)
  }

  def verbalize(frames: FrameSet, plan: DocumentPlan): GeneratedText = {
    // helper functions
    def getFrame(fd: java.util.Map[String, Object], key: String): Frame = {
      fd.get(key) match {
        case s: String => if (fd.containsKey(s))
          getFrame(fd, s)
        else
          frames.getFrame(s)
        case m: java.util.Map[String, Object] =>
          frames.getFrame(m.get("object-id").toString)
      }
    }
    def getVariable(fd: java.util.Map[String, Object], key: String): String =
      fd(fd(key).toString).toString;
    def getVariable2(fd: java.util.Map[String, Object], key: String, subKey: String): String =
      fd(fd(key).asInstanceOf[java.util.Map[String, Object]].get(subKey).toString).toString;
    def typeStrToMagnitude(s: String) = RelativeMagnitude.values.filter(v => v.typeStr.equals(s)).head
    def verbalizeMagnitude(m: RelativeMagnitude.RelativeMagnitude)(implicit _type: String) = _type match {
      case "c-size" => sizeToStr(m)
      case "c-density" => densityToStr(m)
    }
    def templateClause(clause: java.util.Map[String, Object]): Sentence = {
      def verbalize(obj: Object): String =
        obj match {
          case s: String => if (s.startsWith("\"")) s.substring(1, s.length() - 1) else s
          case m: java.util.Map[String, Object] => {
            val fd = frames.getFrame(m.get("object-id").toString)
            if (ontology.isA(fd.getType(), "c-distance")) {
              val components = fd.get("component")
              val component1 = components(0).asInstanceOf[Frame]
              val component2 = components(1).asInstanceOf[Frame]
              s"between ${component1.get("name").head} and ${component2.get("name").head}"
            } else if (fd.containsKey("name")) {
              fd.get("name").head.toString
            } else {
              fd.toString
            }
          }
          case x => x.toString() + " [" + x.getClass() + "]"
        }
      val template = clause.get("template").toString();
      val instantiated = new StringBuffer();
      val fields = template.split("\\@");
      instantiated.append((if (fields(0).startsWith("\"")) fields(0).substring(1) else fields(0)));
      for (i <- 1.to(fields.length - 1)) {
        val keyRest = fields(i).split("\\.", 2)
        val key = keyRest(0)
        val rest = keyRest(1)
        val trimRest = if (i == fields.length - 1 && rest.endsWith("\"")) rest.substring(0,
          rest.length() - 1)
        else rest
        if (clause.containsKey(key) && clause.get(key) != null)
          instantiated.append(verbalize(clause.get(key)));

        instantiated.append(trimRest);
      }
      instantiated.append(".")
      Sentence(instantiated.toString())
    }
    def templateClauses(clauses: java.util.List[java.util.Map[String, Object]]): List[Sentence] =
      clauses.filter { _.containsKey("template") }.map(templateClause).toList

    new GeneratedText(plan.getParagraphs().map {
      para =>
        Paragraph(para.map {
          aggrSegment =>

            // introductory sentence
            if (aggrSegment.size() == 1 && aggrSegment.get(0).get("pred").equals("c-conjunction")) {
              val fd = aggrSegment.get(0)
              val p = nlgFactory.createClause();
              p.setSubject("there");
              p.getSubject().setFeature("expletive_subject", false);
              //              p.getObject().setFeature(Feature.RAISE_SPECIFIER, true)
              p.setVerb("be");
              p.getVerbPhrase().setRealisation("are");
              //              p.setFeature(Feature.PASSIVE, true)
              p.getSubject().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
              p.getVerb().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
              p.getVerbPhrase().setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
              val phrCom = nlgFactory.createNounPhrase("component");
              val numCom = Integer.parseInt(getVariable2(fd, "0", "pred1"));
              phrCom.setSpecifier(numToStr(numCom));
              if (numCom > 1)
                phrCom.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
              val phrDim = nlgFactory.createNounPhrase("dimension");
              val numDim = Integer.parseInt(getVariable2(fd, "1", "pred1"));
              phrDim.setSpecifier(numToStr(numDim));
              phrDim.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);

              p.setObject(new CoordinatedPhraseElement(phrCom, phrDim));
              //              realiser.setDebugMode(true)
              List(Sentence(realiser.realiseSentence(p)))
            } else // if all entries in aggregation set have the same attribute
            if (aggrSegment.forall(clause => clause.get("pred").equals("has-attribute"))) {
              implicit val _type = getFrame(aggrSegment.get(0), "pred1").getType.toString
              if (_type.equals("c-distance")) {
                // distances are a completely different animal, two strategies: 
                // (1) order by distance (components one, two and three are far from each other)
                // (2) order by component (component one is near all the other, 
                //     component two is close to component three)
                
                templateClauses(aggrSegment)
              } else {
                // order them by magnitude value
                val sorted = aggrSegment.sortBy[String](clause => getVariable(clause, "pred2"));
                val c = nlgFactory.createCoordinatedPhrase();
                var current: Pair[List[NPPhraseSpec], RelativeMagnitude.RelativeMagnitude] = null;
                def addToPhrase() = if (current != null) {
                  c.addCoordinate(
                    nlgFactory.createClause(
                      if (current._1.size > 1) {
                        val cc = new CoordinatedPhraseElement()
                        current._1.foreach(cc.addCoordinate(_))
                        cc
                      } else
                        current._1(0),
                      "be", verbalizeMagnitude(current._2)))
                }
                sorted.foreach {
                  clause =>
                    val np = nlgFactory.createNounPhrase("component " +
                      getFrame(clause, "pred0").get("name").head.toString)
                    val magnitude = typeStrToMagnitude(getVariable(clause, "pred2"))
                    if (current == null)
                      current = Pair(List(np), magnitude)
                    else if (magnitude == current._2)
                      current = Pair(current._1 ++ List(np), current._2)
                    else {
                      addToPhrase
                      current = Pair(List(np), magnitude)
                    }
                }
                addToPhrase()

                List(Sentence(realiser.realiseSentence(c)))
              }

              //TODO all about the same component
              //TODO if all entries talk about the same entity, glue them together but keep an eye to mix with the next one
            } else
              // unknown, go template route
              templateClauses(aggrSegment)

        }./:(List[Sentence]())(_ ++ _))
    }.toList)
  }
}

