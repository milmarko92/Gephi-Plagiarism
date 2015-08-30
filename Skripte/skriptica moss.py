
import urllib.request
import re, mmap
import sys, os

def createkey(orig, id, ktype, name, atype):
	output = str.replace(orig, "KEYID", id);
	output = str.replace(output, "KEYTYPE", ktype);
	output = str.replace(output, "ATTRNAME", name);
	output = str.replace(output, "ATTRTYPE", atype);
	return output

def createnode(orig, id):
	output = str.replace(orig, "NODEID", id);
	return output
def createdatakey(orig, id, value):
	output = str.replace(orig, "KEYID", id);
	output = str.replace(output, "KEYVALUE", value)
	return output

def createedge(orig, src, dest):
	output = str.replace(orig, "SOURCEID", src);
	output = str.replace(output, "TARGETID", dest);
	return output


if len(sys.argv) == 1:
	print("You need to enter the moss result page and the results output page")
	exit()
#-o output, -i input 
if '-o' in sys.argv:
	outIndex = sys.argv.index('-o')
else:
	print('You need to set an output folder')
	exit()

outputFolder = sys.argv[outIndex + 1]
if not os.path.isdir(outputFolder):
	try:
		os.mkdir(outputFolder)
	except:
		print('You need to set a valid output Folder')
		exit()


if '-i' in sys.argv:
	inIndex = sys.argv.index('-i')
else:
	print('You need to set a MOSS results page as input')
	exit()
inputPage = sys.argv[inIndex + 1]

user_agent = 'Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-US) AppleWebKit/534.3 (KHTML, like Gecko) Chrome/6.0.472.63 Safari/534.3'
headers = { 'User-Agent' : user_agent }
req = urllib.request.Request(inputPage, None, headers)
try:
	response = urllib.request.urlopen(req)
except URLError:
	print('You need to enter a valid URL as input')
	exit()
page = response.read()
page = str(page)
#print(page)
edges = []
nodenames = []
mo1 = re.findall('<TR><TD><A HREF="([a-z/0-9\.:]*)">(.*?) \(([0-9]*)%\)</A>\\\\n\s*<TD><A HREF="[a-z/0-9\.:]*">(.*?) \(([0-9]*)%\)</A>', page)
print(len(mo1))
for j in mo1:
	#print(j)
	if j[1] not in nodenames:
		nodenames.append(j[1])
	#	print(j[1])
	
	if j[3] not in nodenames:
		nodenames.append(j[3]);
	#	print(j[3])
	edges.append((j[1], j[3], str((float(j[2])+float(j[4]))/2), j[0]))
print(len(edges))


out = open(outputFolder+"\\MOSSoutput.graphml", "w")
out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n")

keytemplate = "\t<key id=\"KEYID\" for=\"KEYTYPE\" attr.name=\"ATTRNAME\" attr.type=\"ATTRTYPE\" />"
nodetemplate = "\t\t<node id=\"NODEID\">"
datakeytemplate = "\t\t\t<data key=\"KEYID\">KEYVALUE</data>"
edgetemplate = "\t\t<edge source=\"SOURCEID\" target=\"TARGETID\">"


keyline = createkey(keytemplate,"NodeName", "node", "Student Name", "string")
out.write(keyline+"\n");
keyline = createkey(keytemplate,"nodeLink", "node", "Link to assignment", "string");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"EdgeLink", "edge", "Link to similarities", "string");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"weight", "edge", "weight", "double");
out.write(keyline+"\n");


out.write("\t<graph edgedefault=\"undirected\">\n");

for nodename in nodenames:
	nodeline = createnode(nodetemplate, nodename);
	out.write(nodeline+"\n")
	datakeyline = createdatakey(datakeytemplate, "NodeName", nodename);
	out.write(datakeyline+"\n")
	out.write("\t\t</node>\n")



for edge in edges:
	edgeline = createedge(edgetemplate, edge[0], edge[1])
	out.write(edgeline+"\n")
	datakeyline = createdatakey(datakeytemplate, "weight", edge[2]);

	out.write(datakeyline+"\n")
	datakeyline = createdatakey(datakeytemplate, "EdgeLink", edge[3]);
	out.write(datakeyline+"\n");
	out.write("\t\t</edge>\n")
out.write("\t</graph>\n</graphml>")

out.close();






