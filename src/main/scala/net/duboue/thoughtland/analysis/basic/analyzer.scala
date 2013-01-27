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

package net.duboue.thoughtland.analysis.basic

import net.duboue.thoughtland.Components
import net.duboue.thoughtland.Analysis
import net.duboue.thoughtland.ComponentAnalyzer
import net.duboue.thoughtland.Environment
import net.duboue.thoughtland.Finding
import net.duboue.thoughtland.ComponentDensity
import net.duboue.thoughtland.ComponentSize
import net.duboue.thoughtland.ComponentDistance
import net.duboue.thoughtland.RelativeMagnitude._
import net.duboue.thoughtland.ComponentDensity

class BasicAnalyzer extends ComponentAnalyzer {

  def logGamma(x: Double): Double = {
    val tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
    val ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
    +24.01409822 / (x + 2) - 1.231739516 / (x + 3)
    +0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
    return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
  }

  def gamma(x: Double): Double = Math.exp(logGamma(x))

  def apply(clusters: Components)(implicit env: Environment): Analysis = {
    System.out.println("Analyzer starting")

    val numComponents = clusters.parts.length
    val n = clusters.main.center.length
    val nDiv2 = n / 2.0
    val cn = Math.pow(Math.PI, nDiv2) / gamma(nDiv2 + 1)
    var fullVolume = cn
    for (i <- 0.to(n - 1)) {
      fullVolume *= clusters.main.radii(i)
    }
    val totalPoints = clusters.main.coveredPoints
    val mainDensity = totalPoints / fullVolume

    val findings = new scala.collection.mutable.ArrayBuffer[Finding]

    for (i <- 0.to(numComponents - 1)) {
      System.out.println(s"Analyzing component $i")

      val component = clusters.parts(i)
      val covered = component.coveredPoints
      var volume = cn
      for (j <- 0.to(n - 1)) {
        volume *= component.radii(j)
      }
      if (volume > fullVolume / 2)
        findings += ComponentSize(i, VeryBig)
      else if (volume > fullVolume / 10)
        findings += ComponentSize(i, Big)

      val density = covered / volume
      if (density > mainDensity)
        findings += ComponentDensity(i, Big)
      else if (density < mainDensity / 2)
        findings += ComponentDensity(i, Small)

      for (j <- (i + 1).to(numComponents - 1)) {
        val other = clusters.parts(j)
        var big = false
        var medium = false
        for (k <- 0.to(n - 1)) {
          val delta = Math.abs(component.center(k) - other.center(k))
          val main = 3 * clusters.main.radii(k)
          if (delta > main) {
            big = true
          } else if (delta > main / 10)
            medium = true
        }

        if (big)
          findings += ComponentDistance(i, j, Big)
        else if (medium)
          findings += ComponentDistance(i, j, Medium)
        else
          findings += ComponentDistance(i, j, Small)
      }
    }
    System.out.println("Done analysis")

    Analysis(numComponents, n, findings.toList)
  }

}
