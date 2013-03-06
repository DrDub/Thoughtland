<%@ page import="net.duboue.thoughtland.ui.servlet.ServletState" %>
<html>
<head>
<title>Thoughtland</title>
</head>
<body>
<h1><img src="images/fl.png">Thoughtland</h1>


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

<h2>Submit a Weka ARFF file for analysis</h2>

<form action="/tl/submission/new" method="POST" enctype-"multipart/form-data">

<p>Algorithm to use: <input type="text" size="50" name="algo"></input></p>
<p>Algorithm parameters: <input type="text" size="100" name="params"></input></p>

<p>Submit a Weka ARFF file for analysis (maximum 500k):<br> <input type="file" name="upload_file> </p>

<p>Please note, in a public server everybody will be able to access (and download) your submission (contact the 
administrator to have the file removed if you submit something by mistake).</p>

<p>How do you want to be identified (alias, name, Twitter handle, email, leave blank for anonymous):<br>
<input type="text" size="50" name="name"></input>
</p>

<p>Any extra text to be associated with the submission (for example, where did the file came from):<br>
<textarea rows="10" cols="80" name="extra"></textarea>
</p>

<p>Any private comments to the administrator (for example, say hi):<br>
<textarea rows="10" cols="80" name="private"></textarea>
</p>

<p><input type="submit"></p>

<h2>Past submissions</h2>

<ol>
<% for(int i=0; i<ServletState.runIds().length; i++) { %>

<li><a href="tl/submission/<%= i %>"><%= ServletState.runDescription(i) %></a></li>

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
<a href="http://thoughtland.duboue.net">http://thoughtland.duboue.net</a><br>
Thoughtland server administered by <b><%= ServletState.prop().getProperty("admin") %></b> <br>

</body>
</html>