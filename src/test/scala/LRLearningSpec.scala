package org.dennybritz.sampler.test

import org.scalatest._
import org.dennybritz.sampler._
import Matchers._

class LRLearningSpec extends FunSpec {

  describe("Learning weight for a logistic Regression model") {

    // One weight, 800 positive and 200 negative evidence variables. One factor for each
    val weights = List(Weight(0, 1.38, false))
    val positiveEvidence = (0 until 800) map (i => BooleanVariable(i, 1.0, true, false))
    val negativeEvidence = (800 until 1000) map (i => BooleanVariable(i, 0.0, true, false))
    val factors = (0 until 1000) map { i =>
      Factor(i, List(FactorVariable(i, true)), 0, ImplyFactorFunction)
    }
    val dataInput = DataInput(
      weights.map (w => (w.id, w)) toMap,
      (positiveEvidence ++ negativeEvidence) map (v => (v.id, v)) toMap,
      factors map (f => (f.id, f)) toMap
    )

    it("should work") {
      val context = GraphContext.create(dataInput)
      val learner = new Learner(context)
      val result = learner.learnWeights(100, 10, 0.1, 0.01, 0.96)
      Console.println(result(0))
      assert(result(0) === (1.38 +- 0.1))
    }

  }

}