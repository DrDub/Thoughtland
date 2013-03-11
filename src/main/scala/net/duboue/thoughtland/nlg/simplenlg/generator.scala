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

import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentDistance
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Finding
import net.duboue.thoughtland.GeneratedText
import net.duboue.thoughtland.Generator
import net.duboue.thoughtland.Paragraph
import net.duboue.thoughtland.Sentence
import net.duboue.thoughtland.RelativeMagnitude
import net.sf.openschema.RDFOntology
import net.sf.openschema.Ontology
import net.sf.openschema.OpenSchemaPlanner
import net.sf.openschema.SimpleFocusChooser
import net.sf.openschema.util.SchemaToXmlFilterStream
import net.sf.openschema.GreedyChooser
import org.xml.sax.InputSource
import net.sf.openschema.FrameSet
import java.io.InputStream
import net.sf.openschema.DocumentPlan
import scala.collection.JavaConversions._
import net.sf.openschema.Frame
import net.duboue.thoughtland.nlg.BasicVerbalizations
import simplenlg.realiser.english.Realiser
import simplenlg.lexicon.Lexicon
import simplenlg.framework.NLGFactory
import simplenlg.framework.CoordinatedPhraseElement
import simplenlg.features.Feature
import simplenlg.features.NumberAgreement
import simplenlg.phrasespec.NPPhraseSpec
import net.duboue.thoughtland.GeneratedText

class SimpleNlgGenerator extends Generator with BasicVerbalizations {

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

  def analysisToFrameSet(analysis: Analysis): FrameSet = new FrameSet() {

    case class MyFrame(id: String, _type: String) extends Frame {
      val map: collection.mutable.Map[String, List[Object]] = new collection.mutable.HashMap[String, List[Object]]();

      def add(key: String, value: Object): Unit = map += key -> (map.get(key).getOrElse(List()) ++ List(value));
      def containsKey(key: String): Boolean = if (key.equals("#ID") || key.equals("#TYPE")) true else map.containsKey(key);
      def get(key: String): java.util.List[Object] = key match {
        case "#ID" => List(id)
        case "#TYPE" => List(_type)
        case _ => map(key)
      }
      def getID(): String = id;
      def getType(): Object = _type;
      def keySet(): java.util.Set[String] = map.keySet
      def set(key: String, vals: java.util.List[Object]): Unit = map += key -> vals.toList;
      def set(key: String, value: Object): Unit = map += key -> List(value);
      def setID(x$1: String): Unit = throw new UnsupportedOperationException();
      def setType(x$1: Any): Unit = throw new UnsupportedOperationException();

      def set(f: (collection.mutable.Map[String, List[Object]]) => Unit): MyFrame =
        { f(map); this }
    }

    case class FrameRef(id: String) extends Frame {
      def add(key: String, value: Object): Unit = throw new UnsupportedOperationException();
      def containsKey(key: String): Boolean = throw new UnsupportedOperationException();
      def get(key: String): java.util.List[Object] = throw new UnsupportedOperationException();
      def getID(): String = id;
      def getType(): Object = throw new UnsupportedOperationException();
      def keySet(): java.util.Set[String] = throw new UnsupportedOperationException();
      def set(key: String, vals: java.util.List[Object]): Unit = throw new UnsupportedOperationException();
      def set(key: String, value: Object): Unit = throw new UnsupportedOperationException();
      def setID(x$1: String): Unit = throw new UnsupportedOperationException();
      def setType(x$1: Any): Unit = throw new UnsupportedOperationException();
    }

    var frameCounter = 1;
    val allFrames: List[Frame] = List(makeCloudFrame(analysis.numberOfDimensions, analysis.numberOfComponents)) ++
      (1.to(analysis.numberOfDimensions).map(makeComponent(_, analysis.findings)) ++
        analysis.findings.filter(_.isInstanceOf[ComponentDistance]).map(makeDistance(_)))./:(List[Frame]())(_ ++ _);

    //    System.out.println(allFrames)

    val nameToFrame: Map[String, Frame] = allFrames.map { frame => (frame.getID(), frame) }.toMap;

    allFrames.foreach {
      frame =>
        frame match {
          case myframe: MyFrame => {

            val keys = frame.keySet().toList
            keys.foreach {
              key =>
                myframe.map += key -> myframe.map(key).map {
                  obj =>
                    obj match {
                      case FrameRef(name) => nameToFrame(name)
                      case other => other
                    }
                }
            }
          }
        }
    }

    def makeCloudFrame(numberOfDimensions: Int, numberOfComponents: Int): Frame =
      MyFrame("full-cloud", "c-full-cloud").set {
        m =>
          m += "components" -> List[Object](numberOfComponents.asInstanceOf[Object]);
          m += "dimensions" -> List[Object](numberOfDimensions.asInstanceOf[Object]);
          m += "component" -> (1.to(numberOfComponents).map { i => FrameRef(s"component-$i") }).toList
      }

    def makeComponent(component: Int, findings: List[Finding]): List[Frame] = {
      var rest: List[Frame] = List()
      val first = MyFrame(s"component-$component", "c-n-ball").set {
        m =>
          m += "name" -> List(numToStr(component));
          def makeAttribute(typeName: String, magnitude: RelativeMagnitude.RelativeMagnitude): Frame = {
            val magnitudeFrame = MyFrame(s"magnitude-$frameCounter", magnitude.typeStr)
            rest = rest ++ List(magnitudeFrame)
            frameCounter += 1
            val attributeFrame = MyFrame(s"$typeName-$frameCounter", s"c-$typeName").set {
              m =>
                m += "component" -> List(FrameRef(s"component-$component"))
                m += "magnitude" -> List(magnitudeFrame)
            }
            frameCounter += 1
            rest = rest ++ List(attributeFrame)
            attributeFrame
          }

          findings.filter({ f =>
            f match {
              case ComponentSize(c, _) => c == component;
              case _ => false
            }
          }).foreach({
            f =>
              f match {
                case ComponentSize(_, s) => m += "size" -> List(makeAttribute("size", s))
              }
          })
          findings.filter({ f =>
            f match {
              case ComponentDensity(c, _) => c == component;
              case _ => false
            }
          }).foreach({
            f =>
              f match {
                case ComponentDensity(_, d) => m += "density" -> List(makeAttribute("density", d))
              }
          })
      }
      List(first) ++ rest
    }

    def makeDistance(f: Finding): List[Frame] = f match {
      case ComponentDistance(c1, c2, d) =>
        val component1 = c1 + 1
        val component2 = c2 + 1
        val magnitudeFrame = MyFrame(s"magnitude-$frameCounter", d.typeStr)
        frameCounter += 1
        val attributeFrame = MyFrame(s"distance-$frameCounter", s"c-distance").set {
          m =>
            m += "component" -> List(FrameRef(s"component-$component1"), FrameRef(s"component-$component2"))
            m += "magnitude" -> List(magnitudeFrame)
        }
        frameCounter += 1
        List(attributeFrame, magnitudeFrame)
    }

    def getFrame(name: String): net.sf.openschema.Frame = nameToFrame.get(name).getOrElse(null)
    def getFrames(): java.util.Collection[net.sf.openschema.Frame] = allFrames
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
                // distances are completely different animal
                //TODO
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

