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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.Writable;

/**
 * Vertex state for paper-style Linear Threshold Model (LTM) simulation.
 *
 * <p>Fields match Sir's MapReduce LTM record, plus {@code activateRound}
 * for Giraph output (which round the vertex first became active).</p>
 *
 * <p>Output {@code toString()} format (space-separated):</p>
 * <pre>
 * B activateRound credence accWeight beliefChanged
 * </pre>
 */
public class V2LtmVertexValue implements Writable {

  /** Active state: {@code 1} active, {@code -1} inactive. */
  private int state;

  /** Activation threshold θ (credence). */
  private double credence;

  /** True if this vertex should send influence in the current superstep. */
  private boolean beliefChanged;

  /** Accumulated influence weight received so far. */
  private double accWeight;

  /**
   * First superstep in which this vertex became active.
   * Seeds use {@code 0}; never activated stays {@code -1}.
   */
  private long activateRound;

  /** Empty constructor required by Hadoop Writable. */
  public V2LtmVertexValue() {
    this(-1, 0.5, false, 0.0, -1L);
  }

  /**
   * Full constructor.
   *
   * @param state active {@code 1} or inactive {@code -1}
   * @param credence threshold θ
   * @param beliefChanged whether this vertex spreads this round
   * @param accWeight accumulated influence
   * @param activateRound first activation superstep, or {@code -1}
   */
  public V2LtmVertexValue(
      int state,
      double credence,
      boolean beliefChanged,
      double accWeight,
      long activateRound) {
    this.state = state;
    this.credence = credence;
    this.beliefChanged = beliefChanged;
    this.accWeight = accWeight;
    this.activateRound = activateRound;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public boolean isActive() {
    return state == 1;
  }

  public double getCredence() {
    return credence;
  }

  public void setCredence(double credence) {
    this.credence = credence;
  }

  public boolean isBeliefChanged() {
    return beliefChanged;
  }

  public void setBeliefChanged(boolean beliefChanged) {
    this.beliefChanged = beliefChanged;
  }

  public double getAccWeight() {
    return accWeight;
  }

  public void setAccWeight(double accWeight) {
    this.accWeight = accWeight;
  }

  public void addAccWeight(double delta) {
    this.accWeight += delta;
  }

  public long getActivateRound() {
    return activateRound;
  }

  public void setActivateRound(long activateRound) {
    this.activateRound = activateRound;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(state);
    out.writeDouble(credence);
    out.writeBoolean(beliefChanged);
    out.writeDouble(accWeight);
    out.writeLong(activateRound);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    state = in.readInt();
    credence = in.readDouble();
    beliefChanged = in.readBoolean();
    accWeight = in.readDouble();
    activateRound = in.readLong();
  }

  @Override
  public String toString() {
    return state + " " + activateRound + " " + credence + " " +
        accWeight + " " + beliefChanged;
  }
}
