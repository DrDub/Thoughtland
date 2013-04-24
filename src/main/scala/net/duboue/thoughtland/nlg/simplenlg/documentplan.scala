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

import net.sf.openschema.DocumentPlan
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import net.sf.openschema.FrameSet
import net.sf.openschema.Frame
import net.duboue.thoughtland.Sentence

/**
 * Transform an OpenSchema document plan into a well-typed scala object.
 */
trait DocumentPlansAsThoughtlandPlans {
  def asThoughtlandPlan(plan: DocumentPlan, frames: FrameSet): ThoughtlandPlan =
    ThoughtlandPlan(plan.getParagraphs().map { para =>
      PlanParagraph(para.map { aggr =>
        PlanAggr(aggr.map { clause =>
          new PlanClause(clause, frames)
        }.toList)
      }.toList)
    }.toList)
}

case class ThoughtlandPlan(paras: List[PlanParagraph])
case class PlanParagraph(aggr: List[PlanAggr])
case class PlanAggr(clauses: List[PlanClause])
class PlanClause(fd: java.util.Map[String, Object], frames: FrameSet) {
  def getFrame(key: String): Frame = {
    fd.get(key) match {
      case s: String => if (fd.containsKey(s))
        getFrame(fd, s)
      else
        frames.getFrame(s)
      case m: java.util.Map[String, Object] =>
        frames.getFrame(m.get("object-id").toString)
    }
  }

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

  def getVariable(key: String): String =
    fd(fd(key).toString).toString;

  def getVariable2(key: String, subKey: String): String =
    fd(fd(key).asInstanceOf[java.util.Map[String, Object]].get(subKey).toString).toString;

  def getString(key: String): String = fd.get(key).toString()

  def getObject(key: String): Object = fd.get(key)

  def contains(key: String): Boolean = fd.containsKey(key)

  def isDefined(key: String): Boolean = contains(key) && getObject(key) != null

  def hasTemplate() = contains("template")

  def templateClause(verbalize: Object => String): Sentence = {
    val template = getString("template");
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
      if (isDefined(key))
        instantiated.append(verbalize(getObject(key)));

      instantiated.append(trimRest);
    }
    instantiated.append(".")
    Sentence(instantiated.toString())
  }

}
