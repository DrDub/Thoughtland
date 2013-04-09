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
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.mutableMapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet

import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentDistance
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.Finding
import net.duboue.thoughtland.RelativeMagnitude
import net.duboue.thoughtland.nlg.BasicVerbalizations
import net.sf.openschema.Frame
import net.sf.openschema.FrameSet
import net.sf.openschema.RDFOntology

/**
 * Transforms a Thoughtland analysis to a FrameSet suitable to use with OpenSchema.
 */
trait AnalysisAsFrames extends BasicVerbalizations {

  val ontology: RDFOntology

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
      (1.to(analysis.numberOfComponents).map(makeComponent(_, analysis.findings)) ++
        analysis.findings.filter(_.isInstanceOf[ComponentDistance]).map(makeDistance(_)))./:(List[Frame]())(_ ++ _);

    System.out.println(allFrames)

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
}