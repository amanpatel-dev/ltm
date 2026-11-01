/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

import org.apache.giraph.conf.LongConfOption;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.log4j.Logger;

/**
 * My implementation of Single Source Shortest Path.
 */
@Algorithm(
    name = "My Shortest Paths",
    description = "Finds shortest paths from a selected source vertex"
)
public class MySSSPComputation extends BasicComputation<
    LongWritable, DoubleWritable, FloatWritable, DoubleWritable> {

  /** Source vertex id. */
  public static final LongConfOption SOURCE_ID =
      new LongConfOption(
          "MySSSPComputation.sourceId",
          1,
          "Source vertex id");

  /** Class logger. */
  private static final Logger LOG =
      Logger.getLogger(MySSSPComputation.class);

  /**
   * Check whether the vertex is the source.
   *
   * @param vertex Vertex
   * @return true if source vertex
   */
  private boolean isSource(Vertex<LongWritable, ?, ?> vertex) {
    return vertex.getId().get() == SOURCE_ID.get(getConf());
  }

  @Override
  public void compute(
      Vertex<LongWritable, DoubleWritable, FloatWritable> vertex,
      Iterable<DoubleWritable> messages)
      throws IOException {

    if (getSuperstep() == 0) {
      vertex.setValue(new DoubleWritable(Double.MAX_VALUE));
    }

    double minDist =
        isSource(vertex) ? 0.0 : Double.MAX_VALUE;

    for (DoubleWritable message : messages) {
      minDist = Math.min(minDist, message.get());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Vertex " + vertex.getId() +
          " minDist = " + minDist +
          " current = " + vertex.getValue());
    }

    if (minDist < vertex.getValue().get()) {
      vertex.setValue(new DoubleWritable(minDist));

      for (Edge<LongWritable, FloatWritable> edge :
          vertex.getEdges()) {

        double distance =
            minDist + edge.getValue().get();

        if (LOG.isDebugEnabled()) {
          LOG.debug(
              "Send to " +
              edge.getTargetVertexId() +
              " distance " +
              distance);
        }

        sendMessage(
            edge.getTargetVertexId(),
            new DoubleWritable(distance));
      }
    }

    vertex.voteToHalt();
  }
}
