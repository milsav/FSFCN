# Graph clustering feature selection
# author: svc (svc@dmi.uns.ac.rs)
# export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/local/lib"

import igraph as ig
import math

from random import randint

class FCGraph(object):
	def __init__(self, inputFile):
		self.graph = ig.Graph()
		lines = [line.rstrip('\n') for line in open(inputFile)]
		createLink = False
		for line in lines:
			if line == "links":
				createLink = True
			else:
				toks = line.split(",")
				if createLink:
					w = math.fabs(float(toks[2]))
					src = self.graph.vs.find(name=toks[0])
					dst = self.graph.vs.find(name=toks[1])
					self.graph.add_edge(src, dst, **{"weight" : w})
				else:
					self.graph.add_vertex(name=toks[0], **{"label" : toks[1], "r" : float(toks[2])})

		

	def cluster(self, cfg_fg, cfg_lv, cfg_wt, cfg_im):
		fg = rg.graph.community_fastgreedy("weight").as_clustering()
		self.__export(fg, cfg_fg + ".cl")
		self.__attrSelectionClusterDriven(fg, cfg_fg)
		
		lv = rg.graph.community_multilevel("weight")
		self.__export(lv, cfg_lv + ".cl")
		self.__attrSelectionClusterDriven(lv, cfg_lv)
		
		wt = rg.graph.community_walktrap("weight").as_clustering()
		self.__export(wt, cfg_wt + ".cl")
		self.__attrSelectionClusterDriven(wt, cfg_wt)
		
		im = rg.graph.community_infomap("weight", trials = 100)
		self.__export(im, cfg_im + ".cl")
		self.__attrSelectionClusterDriven(im, cfg_im)
	
	def __export(self, clustering, file_name):
		net = clustering.graph
		f = open(file_name, 'w')
		f.write(str(clustering.modularity) + "\n")
		f.write(str(len(clustering)) + "\n")
		for node in net.vs:
			f.write(node["name"] + "," + node["label"] + "," + str(clustering.membership[node.index]) + "\n")
		f.close()

	def __attrSelectionClusterDriven(self, clustering, file_name):
		numSelAttr = 0
		f = open(file_name, 'w')
		for clId in range(len(clustering)):
			cl_graph = clustering.subgraph(clId)
			while len(cl_graph.vs) > 0:
				maxRNode = cl_graph.vs[0]
				for node in cl_graph.vs:
					if node["r"] > maxRNode["r"]:
						maxRNode = node
				
				
				f.write(maxRNode["name"] + "," + maxRNode["label"] + "\n")
				numSelAttr = numSelAttr + 1				
				toDelete = [maxRNode.index]
				toDelete.extend(cl_graph.neighbors(maxRNode.index))
				cl_graph.delete_vertices(toDelete)
		
		f.close()			
	
		

fg = FCGraph("fcn.net")
fg.cluster("fg.cfg", "lv.cfg", "wt.cfg", "im.cfg")
print "OK"





