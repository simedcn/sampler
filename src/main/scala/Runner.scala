package org.dennybritz.sampler

import java.io.File

case class Config(inputWeights: File, inputVariables: File, inputFactors: File, inputEdges: File, 
  outputFile: File, numSamplesInference: Int, learningNumIterations: Int, learningNumSamples: Int, 
  learningRate: Double, diminishRate: Double)

object Runner extends App with Logging {

  val parser = new scopt.OptionParser[Config]("sampler") {
    opt[File]("input-weights") required() valueName("<inputWeights>") action { (x, c) =>
      c.copy(inputWeights = x) } text("Weights file in protobuf format")
    opt[File]("input-variables") required() valueName("<inputVariables>") action { (x, c) =>
      c.copy(inputVariables = x) } text("Variables file in protobuf format")
    opt[File]("input-factors") required() valueName("<inputFactors>") action { (x, c) =>
      c.copy(inputFactors = x) } text("Factors file in protobuf format")
    opt[File]("input-edges") required() valueName("<inputEdges>") action { (x, c) =>
      c.copy(inputEdges = x) } text("Edge file in protobuf format")
    opt[File]('o', "outputFile") required() valueName("<outputFile>") action { (x, c) =>
      c.copy(outputFile = x) } text("output file path (required)")
    opt[Int]('i', "numSamplesInference") valueName("<numSamplesInference>") action { (x, c) =>
      c.copy(numSamplesInference = x) } text("number of samples during inference (default: 100)")
    opt[Int]('l', "learningNumIterations") valueName("<learningNumIterations>") action { (x, c) =>
      c.copy(learningNumIterations = x) } text("number of iterations during weight learning (default: 100)")
    opt[Int]('s', "learningNumSamples") valueName("<learningNumSamples>") action { (x, c) =>
      c.copy(learningNumSamples = x) } text("number of samples per iteration during weight learning (default: 1)")
    opt[Double]("alpha") valueName("<learningRate>") action { (x, c) =>
      c.copy(learningRate = x) } text("the learning rate for gradient descent (default: 0.1)")
    opt[Double]("diminish") valueName("<diminishRate>") action { (x, c) =>
      c.copy(diminishRate = x) } text("the diminish rate for learning (default: 0.95)")
    opt[Int]('t',"threads") valueName("<numThreads>") action { (x, c) =>
      log.warn("Setting the number of threads is no longer supported. " + 
        "It's automatically decided by the JVM. Setting ignored.")
      c
    } text ("This setting is no longer supported and will be ignored. " +
      "The number of threads is automatically decided by the JVM.")
  }

  val config = parser.parse(args, Config(null, null, null, null, null, 100, 100, 1, 0.1, 0.95)).getOrElse{
    System.exit(1)
    throw new RuntimeException("")
  }
  
  log.debug("Parsing input...")
  val parserInput = ProtobufInput(config.inputWeights.getCanonicalPath, config.inputVariables.getCanonicalPath,
    config.inputFactors.getCanonicalPath, config.inputEdges.getCanonicalPath)
  val dataInput = ProtobufInputParser.parse(parserInput)
  
  log.debug("Creating factor graph...")
  val graphContext = GraphContext.create(dataInput)
  
  log.debug("Starting learning phase...")
  val learner = new Learner(graphContext)
  val weightsResult = learner.learnWeights(
    config.learningNumIterations, config.learningNumSamples,
    config.learningRate, 0.01, config.diminishRate)
  FileWriter.dumpWeights(weightsResult, config.outputFile.getCanonicalPath + ".weights")
  
  log.debug("Performing inference...")
  val sampler = new Sampler(graphContext)
  val inferenceResult = sampler.calculateMarginals(config.numSamplesInference, 
    graphContext.variables)
  FileWriter.dumpVariables(inferenceResult.variables, config.outputFile.getCanonicalPath)



}