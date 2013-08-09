<%@ page import="net.duboue.thoughtland.ui.servlet.ServletState" %>
<html>
<head>
<title>Thoughtland</title>
<link rel="stylesheet" type="text/css" href="style.css"></link>
</head>
<body background="images/wall4.png">
<h1><img id="logo" src="images/fl.png">Thoughtland<br>
<a href="http://en.wikisource.org/wiki/Flatland_(second_edition)/Section_22"><small><i>I spoke not of a physical Dimension, <br/>
but of a Thoughtland whence, in theory,<br/> 
a Figure could look down upon Flatland <br/>
and see simultaneously the insides of <br/>
all things</i></small></a>
</h1>

<a href="https://github.com/DrDub/Thoughtland"><img style="position: absolute; top: 0; right: 0; border: 0;" src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png" alt="Fork me on GitHub"></a>

<div id="main">
<p>Thoughtland is an end-to-end system that produces an English
text summarizing the error function of a machine learning
algorithm applied to some training data.</p>

<p>Thoughtland is a four stages pipeline:

<dl>
<dt>Cloud</dt><dd>Cloud of points generation via cross-validation</dd>
<dt>Cluster</dt><dd>Model-based clustering to identify interesting components of the error function</dd> 
<dt>Analysis</dt><dd>Identify further relation between the key components</dd>
<dt>Generation</dt><dd>Natural language generation to produce an English text summarizing the error function</dd>
</dl>
</p>

<p>Thoughtland is <a href="http://www.gnu.org/philosophy/free-sw.html">Free Software</a>, 
distributed under the terms of the <a href="http://www.gnu.org/licenses/agpl-3.0.html">Affero GPL v3+</a>.
</p>

<h2>Submit a Weka ARFF file for analysis</h2>

<form action="/tl/submission/new" method="POST" enctype="multipart/form-data">

<p><label>Algorithm to use</label>:<br/>
<input type="text" size="100" name="algo"></input>
<% if(ServletState.isLocked()) { %>
<br><i>This is a public server, only the following algorithms are accepted:</i><br>
<ul>
<% for(String algo: ServletState.getLockedAlgos()) { %>
<li><tt><%= algo %></tt></li>
<% } %>
</ul>
<% } %>
</p>
<p><label>Algorithm parameters</label>:<br/>
<input type="text" size="100" name="params"></input>
<% if(ServletState.isLocked()) { %>
<br><i>This is a public server, only the following parameters are accepted:</i><br>
<ul>
<% for(String param: ServletState.getLockedParams()) { %>
<li><tt><%= param %></tt></li>
<% } %>
</ul>
<% } %>
</p>

<p><label>Submit a Weka ARFF file for analysis</label> 
(maximum <%= ServletState.getProperties().getProperty("maxSizeStr") %>):<br/> 
<input type="file" name="upload_file"> </p>

<p>Please note, in a public server everybody will be able to access (and download) your submission (contact the 
administrator to have the file removed if you submit something by mistake).</p>

<p><label>How do you want to be identified</label> (alias, name, Twitter handle, email, leave blank for anonymous):<br/>
<input type="text" size="100" name="name"></input>
</p>

<p><label>Any extra text to be associated with the submission</label> (for example, where did the file came from):<br/>
<textarea rows="10" cols="80" name="extra"></textarea>
</p>

<p><label>Any private comments to the administrator</label> (for example, say hi):<br/>
<textarea rows="10" cols="80" name="private"></textarea>
</p>

<p><input type="submit"></p>
</form>

<h2>Past submissions</h2>

<ol>
<% for(int i=0; i<ServletState.runIds().length; i++) { %>

<li><a href="tl/submission/<%= i %>#last"><%= ServletState.runDescription(i) %></a></li>

<% } %>
</ol>

<p> &nbsp; </p>
<p> &nbsp; </p>
<p> &nbsp; </p>
<p> &nbsp; </p>
<p> &nbsp; </p>
<p> &nbsp; </p>
<hr>
Thoughtland - Describing n-dimensional Objects<br>
Copyright (C) 2013 Pablo Ariel Duboue<br>
<a href="http://thoughtland.duboue.net">http://thoughtland.duboue.net</a><br>
Thoughtland server administered by <b><%= ServletState.prop().getProperty("admin") %></b> <br>
</div>

</body>
</html>