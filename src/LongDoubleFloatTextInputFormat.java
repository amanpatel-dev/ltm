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

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.giraph.conf.ImmutableClassesGiraphConfigurable;
import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.edge.EdgeFactory;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.TextVertexInputFormat;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * Reads a simple weighted adjacency list from ordinary text files.
 *
 * <p>Expected line format:</p>
 * <pre>
 * vertexId neighborId:weight neighborId:weight ...
 * 1 2:1 6:1
 * </pre>
 *
 * <p>The first token is a long vertex ID. Every remaining token describes one
 * outgoing edge. The file extension is irrelevant; Giraph uses this class
 * because its name is supplied with the {@code -vif} option.</p>
 */
public class LongDoubleFloatTextInputFormat extends TextVertexInputFormat<
    LongWritable, DoubleWritable, FloatWritable> implements
    ImmutableClassesGiraphConfigurable<
        LongWritable, DoubleWritable, FloatWritable> {

  /** Giraph configuration used to create vertices of the configured type. */
  private ImmutableClassesGiraphConfiguration<
      LongWritable, DoubleWritable, FloatWritable> configuration;

  @Override
  public TextVertexReader createVertexReader(
      InputSplit split, TaskAttemptContext context) throws IOException {
    return new LongDoubleFloatVertexReader();
  }

  @Override
  public void setConf(ImmutableClassesGiraphConfiguration<
      LongWritable, DoubleWritable, FloatWritable> conf) {
    this.configuration = conf;
  }

  @Override
  public ImmutableClassesGiraphConfiguration<
      LongWritable, DoubleWritable, FloatWritable> getConf() {
    return configuration;
  }

  /** Converts the current input line into one Giraph vertex. */
  public class LongDoubleFloatVertexReader extends TextVertexReader {
    /** One or more spaces/tabs separate tokens. */
    private final Pattern tokenSeparator = Pattern.compile("\\s+");
    /** A colon separates an edge's target ID from its numeric weight. */
    private final Pattern edgeSeparator = Pattern.compile(":");

    @Override
    public Vertex<LongWritable, DoubleWritable, FloatWritable>
        getCurrentVertex() throws IOException, InterruptedException {
      String line = getRecordReader().getCurrentValue().toString().trim();
      if (line.isEmpty()) {
        throw new IllegalArgumentException("Empty graph input line");
      }

      String[] tokens = tokenSeparator.split(line);
      long vertexIdNumber = Long.parseLong(tokens[0]);
      List<Edge<LongWritable, FloatWritable>> outgoingEdges =
          Lists.newArrayListWithCapacity(tokens.length - 1);

      for (int tokenIndex = 1; tokenIndex < tokens.length; tokenIndex++) {
        String[] edgeParts = edgeSeparator.split(tokens[tokenIndex], -1);
        if (edgeParts.length != 2) {
          throw new IllegalArgumentException(
              "Invalid edge token '" + tokens[tokenIndex] +
              "' in line: " + line);
        }

        long targetVertexId = Long.parseLong(edgeParts[0]);
        float edgeWeight = Float.parseFloat(edgeParts[1]);
        outgoingEdges.add(EdgeFactory.create(
            new LongWritable(targetVertexId),
            new FloatWritable(edgeWeight)));
      }

      Vertex<LongWritable, DoubleWritable, FloatWritable> vertex =
          configuration.createVertex();
      // Algorithms replace the initial value, so start at 0.0.
      vertex.initialize(
          new LongWritable(vertexIdNumber),
          new DoubleWritable(0.0),
          outgoingEdges);
      return vertex;
    }

    @Override
    public boolean nextVertex() throws IOException, InterruptedException {
      return getRecordReader().nextKeyValue();
    }
  }
}
