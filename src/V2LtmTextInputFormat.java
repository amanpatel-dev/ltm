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
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * Reads paper-style LTM node records for {@link V2LinearThresholdComputation}.
 *
 * <p>Expected line format (tab between id and payload):</p>
 * <pre>
 * nodeId\tB;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
 * 1\t1;0.5 2:0.6 3:0.5;true;0.0
 * </pre>
 *
 * <p>After the tab, four {@code ;}-separated pieces are:</p>
 * <ol>
 *   <li>{@code B} — {@code 1} active / {@code -1} inactive</li>
 *   <li>{@code credence} then optional {@code nb:w} tokens</li>
 *   <li>{@code beliefChanged} — {@code true}/{@code false}</li>
 *   <li>{@code accWeight}</li>
 * </ol>
 *
 * <p>Seeds ({@code B == 1}) get {@code activateRound = 0}. Inactive vertices
 * start with {@code activateRound = -1}.</p>
 */
public class V2LtmTextInputFormat extends TextVertexInputFormat<
    LongWritable, V2LtmVertexValue, FloatWritable> implements
    ImmutableClassesGiraphConfigurable<
        LongWritable, V2LtmVertexValue, FloatWritable> {

  /** Giraph configuration used to create vertices of the configured type. */
  private ImmutableClassesGiraphConfiguration<
      LongWritable, V2LtmVertexValue, FloatWritable> configuration;

  @Override
  public TextVertexReader createVertexReader(
      InputSplit split, TaskAttemptContext context) throws IOException {
    return new V2LtmVertexReader();
  }

  @Override
  public void setConf(ImmutableClassesGiraphConfiguration<
      LongWritable, V2LtmVertexValue, FloatWritable> conf) {
    this.configuration = conf;
  }

  @Override
  public ImmutableClassesGiraphConfiguration<
      LongWritable, V2LtmVertexValue, FloatWritable> getConf() {
    return configuration;
  }

  /** Converts one paper-style input line into a Giraph vertex. */
  public class V2LtmVertexReader extends TextVertexReader {
    /** Spaces separate credence from neighbour:weight tokens. */
    private final Pattern tokenSeparator = Pattern.compile("\\s+");
    /** Colon separates neighbour id from weight. */
    private final Pattern edgeSeparator = Pattern.compile(":");

    @Override
    public Vertex<LongWritable, V2LtmVertexValue, FloatWritable>
        getCurrentVertex() throws IOException, InterruptedException {
      String line = getRecordReader().getCurrentValue().toString().trim();
      if (line.isEmpty()) {
        throw new IllegalArgumentException("Empty LTM input line");
      }

      String[] idAndRest = splitIdAndPayload(line);
      long vertexIdNumber = Long.parseLong(idAndRest[0].trim());
      String payload = idAndRest[1].trim();

      // B ; credence nb:w... ; beliefChanged ; accWeight
      String[] major = payload.split(";", -1);
      if (major.length != 4) {
        throw new IllegalArgumentException(
            "LTM payload must have four ';' fields "
                + "(B;credence[ nb:w...];beliefChanged;accWeight): " + line);
      }

      int state = Integer.parseInt(major[0].trim());
      if (state != 1 && state != -1) {
        throw new IllegalArgumentException(
            "LTM state B must be 1 or -1 in line: " + line);
      }

      String credenceAndEdges = major[1].trim();
      boolean beliefChanged = Boolean.parseBoolean(major[2].trim());
      double accWeight = Double.parseDouble(major[3].trim());

      String[] credenceTokens = tokenSeparator.split(credenceAndEdges);
      if (credenceTokens.length < 1 || credenceTokens[0].isEmpty()) {
        throw new IllegalArgumentException(
            "LTM credence missing in line: " + line);
      }

      double credence = Double.parseDouble(credenceTokens[0]);
      List<Edge<LongWritable, FloatWritable>> outgoingEdges =
          Lists.newArrayListWithCapacity(
              Math.max(0, credenceTokens.length - 1));

      for (int i = 1; i < credenceTokens.length; i++) {
        String[] edgeParts = edgeSeparator.split(credenceTokens[i], -1);
        if (edgeParts.length != 2) {
          throw new IllegalArgumentException(
              "Invalid edge token '" + credenceTokens[i] + "' in line: " +
                  line);
        }
        long targetVertexId = Long.parseLong(edgeParts[0]);
        float edgeWeight = Float.parseFloat(edgeParts[1]);
        outgoingEdges.add(EdgeFactory.create(
            new LongWritable(targetVertexId),
            new FloatWritable(edgeWeight)));
      }

      long activateRound = state == 1 ? 0L : -1L;
      V2LtmVertexValue value = new V2LtmVertexValue(
          state, credence, beliefChanged, accWeight, activateRound);

      Vertex<LongWritable, V2LtmVertexValue, FloatWritable> vertex =
          configuration.createVertex();
      vertex.initialize(
          new LongWritable(vertexIdNumber),
          value,
          outgoingEdges);
      return vertex;
    }

    @Override
    public boolean nextVertex() throws IOException, InterruptedException {
      return getRecordReader().nextKeyValue();
    }

    /**
     * Splits {@code nodeId} from the paper payload. Prefers a tab; falls back
     * to the first whitespace so simple editors remain usable.
     */
    private String[] splitIdAndPayload(String line) {
      int tab = line.indexOf('\t');
      if (tab > 0 && tab < line.length() - 1) {
        return new String[] {
            line.substring(0, tab),
            line.substring(tab + 1)
        };
      }
      int ws = indexOfFirstWhitespace(line);
      if (ws <= 0 || ws == line.length() - 1) {
        throw new IllegalArgumentException(
            "LTM line must be 'nodeId<TAB>payload': " + line);
      }
      return new String[] {
          line.substring(0, ws),
          line.substring(ws + 1).trim()
      };
    }

    private int indexOfFirstWhitespace(String line) {
      for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);
        if (c == ' ' || c == '\t') {
          return i;
        }
      }
      return -1;
    }
  }
}
