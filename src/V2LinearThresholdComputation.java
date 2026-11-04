/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.examples;

import java.io.IOException;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;

/**
 * Paper-style Linear Threshold Model on Giraph.
 *
 * <p>Vertex state and seed/threshold come from
 * {@link V2LtmTextInputFormat} records. Only vertices with
 * {@code beliefChanged == true} send influence (edge weights) in a
 * superstep. Receivers add those weights into persistent
 * {@code accWeight} and activate when {@code accWeight >= credence}.</p>
 *
 * <p>Keep {@link LinearThresholdComputation} unchanged as the simple v1
 * draft with hard-coded seed and threshold.</p>
 */
public class V2LinearThresholdComputation extends BasicComputation<
    LongWritable, V2LtmVertexValue, FloatWritable, DoubleWritable> {

  @Override
  public void compute(
      Vertex<LongWritable, V2LtmVertexValue, FloatWritable> vertex,
      Iterable<DoubleWritable> messages) throws IOException {

    V2LtmVertexValue value = vertex.getValue();

    // Newly activated vertices (including seeds from input) spread once.
    if (value.isBeliefChanged()) {
      sendInfluence(vertex);
      value.setBeliefChanged(false);
      vertex.setValue(value);
    }

    double incoming = 0.0;
    for (DoubleWritable message : messages) {
      incoming += message.get();
    }

    if (incoming > 0.0) {
      value.addAccWeight(incoming);
      vertex.setValue(value);
    }

    if (!value.isActive() && value.getAccWeight() >= value.getCredence()) {
      value.setState(1);
      value.setBeliefChanged(true);
      value.setActivateRound(getSuperstep());
      vertex.setValue(value);
    }

    // Stay active for the next superstep if we must spread influence then.
    // Otherwise Giraph would leave a halted vertex that never sends.
    if (!value.isBeliefChanged()) {
      vertex.voteToHalt();
    }
  }

  /**
   * Sends each outgoing edge weight to the neighbour.
   *
   * @param vertex vertex that became active in the previous logical round
   */
  private void sendInfluence(
      Vertex<LongWritable, V2LtmVertexValue, FloatWritable> vertex) {
    for (Edge<LongWritable, FloatWritable> edge : vertex.getEdges()) {
      sendMessage(
          edge.getTargetVertexId(),
          new DoubleWritable(edge.getValue().get()));
    }
  }
}
