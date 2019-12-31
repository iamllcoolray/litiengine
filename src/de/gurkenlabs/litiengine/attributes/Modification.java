package de.gurkenlabs.litiengine.attributes;

/**
 * The different modification types that can be applied to an <code>Attribute</code>.
 * 
 * @see AttributeModifier#getModification()
 */
public enum Modification {
  ADD(1),
  ADDPERCENT(5),
  DIVIDE(4),
  MULTIPLY(3),
  SET(7),
  SUBSTRACT(2),
  SUBSTRACTPERCENT(6),
  UNKNOWN(-1);

  private final int applyOrder;

  private Modification(final int value) {
    this.applyOrder = value;
  }

  public int getApplyOrder() {
    return this.applyOrder;
  }
}
