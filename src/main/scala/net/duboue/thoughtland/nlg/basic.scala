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

package net.duboue.thoughtland.nlg;

import net.duboue.thoughtland.RelativeMagnitude

/**
 * Basic verbalizations shared by most NLG classes.
 */

trait BasicVerbalizations {
  def numToStr(i: Int): String = i match {
    case 0 => "zero"
    case 1 => "one"
    case 2 => "two"
    case 3 => "three"
    case 4 => "four"
    case 5 => "five"
    case 6 => "six"
    case 7 => "seven"
    case 8 => "eight"
    case 9 => "nine"
    case 10 => "ten"
    case _ => i.toString
  }

  def densityToStr(d: RelativeMagnitude.RelativeMagnitude): String = d match {
    case RelativeMagnitude.VeryBig => "very dense"
    case RelativeMagnitude.Big => "dense"
    case RelativeMagnitude.Medium => "somewhat dense"
    case RelativeMagnitude.Small => "sparse"
    case RelativeMagnitude.VerySmall => "very sparse"
  }

  def sizeToStr(s: RelativeMagnitude.RelativeMagnitude): String = s match {
    case RelativeMagnitude.VeryBig => "giant"
    case RelativeMagnitude.Big => "big"
    case RelativeMagnitude.Medium => "normal size"
    case RelativeMagnitude.Small => "small"
    case RelativeMagnitude.VerySmall => "tiny"
  }

  def distanceToStr(d: RelativeMagnitude.RelativeMagnitude): String = d match {
    case RelativeMagnitude.VeryBig => "very far from"
    case RelativeMagnitude.Big => "far from"
    case RelativeMagnitude.Medium => "at a good distance from"
    case RelativeMagnitude.Small => "close to"
    case RelativeMagnitude.VerySmall => "very close to"
  }
}