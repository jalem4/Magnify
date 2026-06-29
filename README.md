# Magnify

An Information Retrieval System written in Java

# Description
A program that uses the BM25 algorithm to rank your local documents and allows you to query them.

## Getting Started


### Dependencies

* Java installed on your computer

### Installing

* ``` git clone git@github.com:jalem4/Magnify.git ```

### Executing program
```
javac ConsoleInterface.java
java ConsoleInterface
```

## Usage
After running ConsoleInterface.java, you will first need to enter your directory of files to scan. You can also click [here](https://github.com/uwgraphics/VEP2_TCP_SimpleText) to download a test directory and use that. This repo contains  simplified .txt format versions of Early Modern English texts as provided by the Visualizing English Print project at University of Wisconsin-Madison. Please note that the directory is quite large, with over 61000 files, so using only a few directories is recommended for quicker processing. Then simply follow the instructions on screen to navigate through and use the program. The menu is as follows:
* 1. Perform search - input a search query
* 2. Build database - create inverted index using provided search directory
* 3. Settings - change number of documents to show, directory to search through, and path to save/load index 
* 4. Save database - save inverted index to .idx file
* 5. Load database - create inverted index from .idx file
* 0. Quit - quit program

## Roadmap
* Finish Evaluator.java to show Precision@K, Recall@K, etc.
* Implement Query Expansion
* Add dense vector embeddings
* Build a GUI for better user experience