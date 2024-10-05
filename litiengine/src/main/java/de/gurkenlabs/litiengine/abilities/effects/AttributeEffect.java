package de.gurkenlabs.litiengine.abilities.effects;

import de.gurkenlabs.litiengine.abilities.targeting.TargetingStrategy;
import de.gurkenlabs.litiengine.attributes.Attribute;
import de.gurkenlabs.litiengine.attributes.AttributeModifier;
import de.gurkenlabs.litiengine.attributes.Modification;
import de.gurkenlabs.litiengine.entities.ICombatEntity;

/**
 * An attribute effect applies an attribute modifier to the affected entity when applied and removes it when ceased.
 *
 * @param <T>
 *          the generic type
 */
public abstract class AttributeEffect<T extends Number> extends Effect {

  /** The modifier. */
  private final AttributeModifier<T> modifier;

  protected AttributeEffect(final TargetingStrategy targetingStrategy,
                            final Modification modification,
                            final double delta) {
    this(targetingStrategy, null, modification, delta);
  }

  protected AttributeEffect(final TargetingStrategy targetingStrategy,
      final ICombatEntity executingEntity,
      final Modification modification,
      final double delta) {
    super(targetingStrategy, executingEntity);
    this.modifier = new AttributeModifier<>(modification, delta);
  }

  @Override
  public void cease(final ICombatEntity affectedEntity) {
    super.cease(affectedEntity);
    this.getAttribute(affectedEntity).removeModifier(this.getModifier());
  }

  public AttributeModifier<T> getModifier() {
    return this.modifier;
  }

  @Override
  protected void apply(final ICombatEntity affectedEntity) {
    super.apply(affectedEntity);
    this.getAttribute(affectedEntity).addModifier(this.getModifier());
  }

  protected abstract Attribute<T> getAttribute(final ICombatEntity entity);
}
