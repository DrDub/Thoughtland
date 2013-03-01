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

package net.duboue.thoughtland.ui.servlet;

import javax.servlet.Servlet

class ThoughtlandServlet extends Servlet {

  var config: javax.servlet.ServletConfig = null

  def init(config: javax.servlet.ServletConfig): Unit = {
    this.config = config
  }

  def getServletConfig(): javax.servlet.ServletConfig = config

  def getServletInfo(): String = "Thoughtland Servlet"

  def service(req: javax.servlet.ServletRequest, res: javax.servlet.ServletResponse): Unit = {
    System.out.println(req);
  }
  def destroy(): Unit = {

  }

}

