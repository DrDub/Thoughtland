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

class SimpleNlgGenerator extends Generator {

  val ontology = new RDFOntology(classOf[SimpleNlgGenerator].getResourceAsStream("ontology.rdfs"),
    "classpath://net/duboue/thoughtland/nlg/simplenlg/ontology.rdfs");

  val byComponentSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-component.schema"))), new SimpleFocusChooser(ontology));
  val byAttributeSchema = new OpenSchemaPlanner(new InputSource(new SchemaToXmlFilterStream(classOf[SimpleNlgGenerator].getResourceAsStream("by-attribute.schema"))), new GreedyChooser());

  def apply(analysis: Analysis)(implicit env: Environment): GeneratedText = {
    val frames = analysisToFrameSet(analysis);
    val texts = List(byComponentSchema, byAttributeSchema)
      .map { _.instantiate(frames, new java.util.HashMap(), ontology) }
      .map(verbalize);
    if (texts(0).toString.length < texts(1).toString.length)
      return texts(0)
    else
      return texts(1)
  }

  protected def analysisToFrameSet(analysis: Analysis): FrameSet = new FrameSet() {
    //TODO
    def getFrame(x$1: String): net.sf.openschema.Frame = null
    def getFrames(): java.util.Collection[net.sf.openschema.Frame] = null
  }

  protected def verbalize(plan: DocumentPlan): GeneratedText =
    new GeneratedText(plan.getParagraphs().map {
      aggrSegments =>
        var clauses: List[java.util.Map[String, Object]] = List();
        for (aggr <- aggrSegments)
          clauses = clauses ++ aggr;
        Paragraph(clauses.filter { !_.containsKey("template") }.map {
          clause =>
            val template = clause.get("template").toString();
            val instantiated = new StringBuffer();
            val fields = template.split("\\@");
            instantiated.append((if (fields(0).startsWith("\"")) fields(0).substring(1) else fields(0)));
            for (i <- 1.to(fields.length)) {
              val nameRest = fields(i).split("\\.", 2);
              if (clause.containsKey(nameRest(0)) && clause.get(nameRest(0)) != null) {
                val value = clause.get(nameRest(0)).toString();
                instantiated.append(if (value.startsWith("\"")) value.substring(1, value.length() - 1) else value);
              }
              instantiated.append(if (i == fields.length - 1 && nameRest(1).endsWith("\"")) nameRest(1).substring(0,
                nameRest(1).length() - 1)
              else nameRest(1));
            }

            Sentence(instantiated.toString())
        })
    }.toList)
}

