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

package net.duboue.thoughtland

/**
 * Architecture interfaces for Thoughtland.
 *
 * The different implementations are in each package.
 *
 * Thoughtland is a pipeline of four components:
 *
 * 1. A CloudExtractor, that extract points by training a machine learning model (or by other means)
 *
 * 2. A Clusterer, that turns the cloud of points into a cluster components + a full set cluster (for references)
 *
 * 3. A ComponentAnalyzer, that takes the cluster components and full set cluster and find interesting relations among them.
 *
 * 4. A Generator, that takes the relations and produces a textual description.
 *
 */

trait CloudExtractor {
  def apply(data: TrainingData, algo: String, params: Array[String])(implicit env: Environment): CloudPoints
}

trait Clusterer {
  def apply(cloud: CloudPoints, numIter: Int)(implicit env: Environment): Components
}

trait ComponentAnalyzer {
  def apply(clusters: Components)(implicit env: Environment): Analysis
}

trait Generator {
  def apply(analyis: Analysis)(implicit env: Environment): GeneratedText
}

// full system
case class Thoughtland(extractor: CloudExtractor, clusterer: Clusterer, analyzer: ComponentAnalyzer, generator: Generator) {
  def apply(data: TrainingData, algo: String, params: Array[String], numIter: Int)(implicit env: Environment): GeneratedText =
    generator(analyzer(clusterer(extractor(data, algo, params), numIter)))
}
