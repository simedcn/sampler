package org.dennybritz.sampler

import java.io.File

case class Config(weightsFile: File, factorsFile: File, variablesFile: File, outputFile: File,
  numSamplesInference: Int, learningNumIterations: Int, learningNumSamples: Int, learningRate: Double,
  diminishRate: Double)

object Runner extends App with Logging {

  val parser = new scopt.OptionParser[Config]("scopt") {
    opt[File]('w', "weights") required() valueName("<weightsFile>") action { (x, c) =>
      c.copy(weightsFile = x) } text("weights File")
    opt[File]('v', "variables") required() valueName("<variablesFile>") action { (x, c) =>
      c.copy(variablesFile = x) } text("variables File")
    opt[File]('f', "factors") required() valueName("<factorsFile>") action { (x, c) =>
      c.copy(factorsFile = x) } text("factors File")
    opt[File]('o', "outputFile") required() valueName("<outputFile>") action { (x, c) =>
      c.copy(outputFile = x) } text("output file")
    opt[Int]('i', "numSamplesInference") required() valueName("<numSamplesInference>") action { (x, c) =>
      c.copy(numSamplesInference = x) } text("number of samples for inference")
    opt[Int]('l', "learningNumIterations") valueName("<learningNumIterations>") action { (x, c) =>
      c.copy(learningNumIterations = x) } text("number of iterations during weight learning")
    opt[Int]('s', "learningNumSamples") valueName("<learningNumSamples>") action { (x, c) =>
      c.copy(learningNumSamples = x) } text("number of samples per iteration during weight learning")
    opt[Double]("alpha") valueName("<learningRate>") action { (x, c) =>
      c.copy(learningRate = x) } text("the learning rate for gradient descent (default: 0.1)")
    opt[Double]("diminish") valueName("<learningRate>") action { (x, c) =>
      c.copy(diminishRate = x) } text("the diminish rate for learning (default: 0.95)")
    opt[Int]('t',"threads") valueName("<numThreads>") action { (x, c) =>
      log.warn("Setting the number of threads is no longer supported. " + 
        "It's automatically decided by the JVM. Setting ignored.")
      c
    } text ("number of threads.")
  }

  val config = parser.parse(args, Config(null, null, null, null, 100, 100, 1, 0.1, 0.95)).getOrElse{
    System.exit(1)
    throw new RuntimeException("")
  }
  
  log.debug("Parsing input...")
  val parserInput = DeepDiveInput(config.factorsFile.getCanonicalPath, config.variablesFile.getCanonicalPath,
    config.weightsFile.getCanonicalPath)
  val dataInput = DeepDiveInputParser.parse(parserInput)
  
  log.debug("Creating factor graph...")
  val graphContext = GraphContext.create(dataInput)
  
  log.debug("Starting learning phrase...")
  val learner = new Learner(graphContext)
  val weightsResult = learner.learnWeights(
    config.learningNumIterations, config.learningNumSamples,
    config.learningRate, 0.01, config.diminishRate)
  FileWriter.dumpWeights(weightsResult, config.outputFile.getCanonicalPath + ".weights")
  
  log.debug("Performing inference...")
  val sampler = new Sampler(graphContext)
  val inferenceResult = sampler.calculateMarginals(config.numSamplesInference, 
    graphContext.variablesMap.values.toSeq)
  FileWriter.dumpVariables(inferenceResult.variables, config.outputFile.getCanonicalPath)



}