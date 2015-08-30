import re, mmap, sys, os

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
	print('You need to set a JPLAG Results Folder as input')
	exit()
inputFolder = sys.argv[inIndex + 1]

if not os.path.isdir(inputFolder):
	print('You need to set a valid input Folder')
	exit()



f = open(inputFolder+'\index.html', 'r+')
file = f.read();
x = file.split("Matches sorted by maximum similarity");

mo = re.findall('<TR><TD BGCOLOR=#[0-9a-f]*>(.*?)</TD>(.*)</TD>', x[0])
edges = []
nodes = set()
nodenames = set()
for i in mo:
	mo1 = re.findall('A HREF="(.*?)">(.*?)</A><BR><FONT COLOR="#[0-9a-f]*">\((.*?)\)</FONT>', i[1])
	
	for j in mo1:
		if i[0] not in nodenames:
			test = j[0][:len(j[0])-5]+"-0"+j[0][len(j[0])-5:]
			nodes.add((i[0], test));
			nodenames.add(i[0])
		
		if j[1] not in nodenames:
			test = j[0][:len(j[0])-5]+"-1"+j[0][len(j[0])-5:]
			nodes.add((j[1], test));
			nodenames.add(j[1]);
		edges.append((i[0], j[1], j[2], j[0]))

f.close();

out = open(outputFolder+"\output.graphml", "w")
out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n")

keytemplate = "\t<key id=\"KEYID\" for=\"KEYTYPE\" attr.name=\"ATTRNAME\" attr.type=\"ATTRTYPE\" />"
nodetemplate = "\t\t<node id=\"NODEID\">"
datakeytemplate = "\t\t\t<data key=\"KEYID\">KEYVALUE</data>"
edgetemplate = "\t\t<edge source=\"SOURCEID\" target=\"TARGETID\">"



keyline = createkey(keytemplate,"NodeName", "node", "Student Name", "string")
out.write(keyline+"\n");
keyline = createkey(keytemplate,"nodeLink", "node", "Link to assignment", "string");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"V-Label", "node", "Label", "string");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"EdgeLink", "edge", "Link to similarities", "string");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"weight", "edge", "weight", "double");
out.write(keyline+"\n");
keyline = createkey(keytemplate,"E-Label", "edge", "Label", "string");
out.write(keyline+"\n");

out.write("\t<graph edgedefault=\"undirected\">\n");

for node in nodes:
	nodeline = createnode(nodetemplate, node[0]);
	out.write(nodeline+"\n")
	datakeyline = createdatakey(datakeytemplate, "NodeName", node[0]);
	out.write(datakeyline+"\n")
	datakeyline = createdatakey(datakeytemplate, "nodeLink", node[1]);
	out.write(datakeyline+"\n");
	datakeyline = createdatakey(datakeytemplate, "V-Label", node[0]);
	out.write(datakeyline+"\n");
	out.write("\t\t</node>\n")



for edge in edges:
	edgeline = createedge(edgetemplate, edge[0], edge[1])
	out.write(edgeline+"\n")
	datakeyline = createdatakey(datakeytemplate, "weight", edge[2][:len(edge[2])-1]);
	out.write(datakeyline+"\n")
	datakeyline = createdatakey(datakeytemplate, "EdgeLink", edge[3]);
	out.write(datakeyline+"\n");
	datakeyline = createdatakey(datakeytemplate, "E-Label", edge[2][:len(edge[2])-1]);
	out.write(datakeyline+"\n")
	
	out.write("\t\t</edge>\n")
out.write("\t</graph>\n</graphml>")

out.close();
exit(0);
