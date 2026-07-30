package me.Thelnfamous1.bettermobcombat.logic;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.Thelnfamous1.bettermobcombat.BetterMobCombat;
import me.Thelnfamous1.bettermobcombat.Constants;
import me.Thelnfamous1.bettermobcombat.api.MobAttackRangeExtensions;
import me.Thelnfamous1.bettermobcombat.api.MobAttackWindup;
import me.Thelnfamous1.bettermobcombat.mixin.MobAccessor;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.WeaponHitBoxes;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.TargetHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.bettercombat.logic.knockback.ConfigurableKnockback;
import net.bettercombat.mixin.LivingEntityAccessor;
import net.bettercombat.utils.MathHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class MobCombatHelper {

    // Better Combat 2.x no longer exposes its internal attack-modifier ids, and 1.21 AttributeModifiers are keyed
    // by ResourceLocation rather than UUID, so we define our own dedicated ids for the mob attack pipeline.
    private static final ResourceLocation COMBO_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("bettermobcombat", "combo_damage");
    private static final ResourceLocation DUAL_WIELDING_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("bettermobcombat", "dual_wielding_damage");
    private static final ResourceLocation SWEEPING_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("bettermobcombat", "sweeping_damage");

    public static ItemStack getDirectMainhand(Mob mob){
        return ((MobAccessor)mob).bettermobcombat$getHandItems().get(EquipmentSlot.MAINHAND.getIndex());
    }

    public static ItemStack getDirectOffhand(Mob mob){
        return ((MobAccessor)mob).bettermobcombat$getHandItems().get(EquipmentSlot.OFFHAND.getIndex());
    }

    public static void onHoldingBetterCombatWeapon(Mob mob, BiConsumer<Mob, WeaponAttributes> callback) {
        if(BetterMobCombat.getServerConfigHelper().isBlacklistedForBetterCombat(mob)){
            return;
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(mob.getMainHandItem());
        if (attributes != null && attributes.attacks() != null) {
            callback.accept(mob, attributes);
        }
    }

    public static boolean canUseBetterCombatWeapon(Mob mob, BiPredicate<Mob, WeaponAttributes> predicate) {
        if(BetterMobCombat.getServerConfigHelper().isBlacklistedForBetterCombat(mob)){
            return false;
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(mob.getMainHandItem());
        if (attributes != null && attributes.attacks() != null) {
            return predicate.test(mob, attributes);
        }
        return false;
    }

    public static <T> T applyWithBetterCombatWeapon(Mob mob, BiFunction<Mob, WeaponAttributes, T> function, Supplier<T> defaultValue) {
        if(BetterMobCombat.getServerConfigHelper().isBlacklistedForBetterCombat(mob)){
            return defaultValue.get();
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(mob.getMainHandItem());
        if (attributes != null && attributes.attacks() != null) {
            return function.apply(mob, attributes);
        }
        return defaultValue.get();
    }

    public static void processAttack(Level world, Mob mob, int comboCount, List<Entity> targets, @Nullable BiConsumer<Mob, Entity> damageApplicator){
        if (world != null && !world.isClientSide) {
            AttackHand hand = MobAttackHelper.getCurrentAttack(mob, comboCount);
            if (hand == null) {
                Constants.LOG.error("Server handling attack for {} - No current attack hand!", mob);
                Constants.LOG.error("Combo count: " + comboCount + " is dual wielding: " + MobAttackHelper.isDualWielding(mob));
                Constants.LOG.error("Main-hand stack: " + mob.getMainHandItem());
                Constants.LOG.error("Off-hand stack: " + mob.getOffhandItem());
            } else {
                WeaponAttributes.Attack attack = hand.attack();
                WeaponAttributes attributes = hand.attributes();
                world.getServer().executeIfPossible(() -> {
                    ((PlayerAttackProperties)mob).setComboCount(comboCount);
                    Multimap<Holder<Attribute>, AttributeModifier> comboAttributes = null;
                    Multimap<Holder<Attribute>, AttributeModifier> dualWieldingAttributes = null;
                    Multimap<Holder<Attribute>, AttributeModifier> sweepingModifiers = HashMultimap.create();
                    int sweepingLevel;
                    if (attributes != null && attack != null) {
                        comboAttributes = HashMultimap.create();
                        double comboMultiplier = attack.damageMultiplier() - 1.0;
                        comboAttributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(COMBO_DAMAGE_MODIFIER_ID, comboMultiplier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                        mob.getAttributes().addTransientAttributeModifiers(comboAttributes);
                        float dualWieldingMultiplier = MobAttackHelper.getDualWieldingAttackDamageMultiplier(mob, hand) - 1.0F;
                        if (dualWieldingMultiplier != 0.0F) {
                            dualWieldingAttributes = HashMultimap.create();
                            dualWieldingAttributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DUAL_WIELDING_MODIFIER_ID, dualWieldingMultiplier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                            mob.getAttributes().addTransientAttributeModifiers(dualWieldingAttributes);
                        }

                        if (hand.isOffHand()) {
                            MobAttackHelper.setAttributesForOffHandAttack(mob, true);
                        }

                        MobSoundHelper.playSound((ServerLevel)world, mob, attack.swingSound());
                        if (BetterCombatMod.config.allow_reworked_sweeping && targets.size() > 1) {
                            // Better Combat 2.x dropped the sweeping-edge damage-restoration mechanic, so the penalty
                            // is now purely a function of the number of extra targets.
                            double multiplier = 1.0 - (double)(BetterCombatMod.config.reworked_sweeping_maximum_damage_penalty / (float)BetterCombatMod.config.reworked_sweeping_extra_target_count * (float)Math.min(BetterCombatMod.config.reworked_sweeping_extra_target_count, targets.size() - 1));
                            multiplier = Math.min(multiplier, 1.0);
                            sweepingModifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(SWEEPING_MODIFIER_ID, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                            mob.getAttributes().addTransientAttributeModifiers(sweepingModifiers);
                            boolean playEffects = !BetterCombatMod.config.reworked_sweeping_sound_and_particles_only_for_swords || hand.itemStack().getItem() instanceof SwordItem;
                            if (BetterCombatMod.config.reworked_sweeping_plays_sound && playEffects) {
                                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, mob.getSoundSource(), 1.0F, 1.0F);
                            }

                            if (BetterCombatMod.config.reworked_sweeping_emits_particles && playEffects) {
                                sweepAttack(mob);
                            }
                        }
                    }

                    float attackCooldown = MobAttackHelper.getAttackCooldownTicksCapped(mob);
                    float knockbackMultiplier = BetterCombatMod.config.knockback_reduced_for_fast_attacks ? MathHelper.clamp(attackCooldown / 12.5F, 0.1F, 1.0F) : 1.0F;
                    int lastAttackedTicks = ((LivingEntityAccessor)mob).getAttackStrengthTicker();

                    for(sweepingLevel = 0; sweepingLevel < targets.size(); ++sweepingLevel) {
                        Entity target = targets.get(sweepingLevel);

                        if (target != null && (!target.equals(mob.getVehicle()) || TargetHelper.isAttackableMount(target)) && (!(target instanceof ArmorStand) || !((ArmorStand)target).isMarker())) {
                            LivingEntity livingTarget = target instanceof LivingEntity ? (LivingEntity) target : null;
                            if (livingTarget != null) {
                                if (BetterCombatMod.config.allow_fast_attacks) {
                                    livingTarget.invulnerableTime = 0;
                                }

                                if (knockbackMultiplier != 1.0F) {
                                    ((ConfigurableKnockback)livingTarget).setKnockbackMultiplier_BetterCombat(knockbackMultiplier);
                                }
                            }

                            ((LivingEntityAccessor)mob).setLastAttackedTicks(lastAttackedTicks);
                            if (target instanceof ItemEntity || target instanceof ExperienceOrb || target instanceof AbstractArrow || target == mob) {
                                Constants.LOG.error("{} tried to attack an invalid entity - {}", mob.getName().getString(), target);
                                return;
                            }

                            if(damageApplicator != null){
                                damageApplicator.accept(mob, target);
                            } else{
                                mob.doHurtTarget(target);
                            }

                            if (livingTarget != null) {
                                if (knockbackMultiplier != 1.0F) {
                                    ((ConfigurableKnockback)livingTarget).setKnockbackMultiplier_BetterCombat(1.0F);
                                }
                            }
                        }
                    }

                    mob.setNoActionTime(0);

                    if (comboAttributes != null) {
                        mob.getAttributes().removeAttributeModifiers(comboAttributes);
                        if (hand.isOffHand()) {
                            MobAttackHelper.setAttributesForOffHandAttack(mob, false);
                        }
                    }

                    if (dualWieldingAttributes != null) {
                        mob.getAttributes().removeAttributeModifiers(dualWieldingAttributes);
                    }

                    if (!sweepingModifiers.isEmpty()) {
                        mob.getAttributes().removeAttributeModifiers(sweepingModifiers);
                    }

                    //((PlayerAttackProperties)mob).setComboCount(-1);
                });
            }
        }
    }

    public static void sweepAttack(Mob mob) {
        double xOffset = -Mth.sin(mob.getYRot() * Mth.DEG_TO_RAD);
        double zOffset = Mth.cos(mob.getYRot() * Mth.DEG_TO_RAD);
        if (mob.level() instanceof ServerLevel) {
            ((ServerLevel)mob.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, mob.getX() + xOffset, mob.getY(0.5D), mob.getZ() + zOffset, 0, xOffset, 0.0D, zOffset, 0.0D);
        }

    }

    public static double calculateAttributeValue(Holder<Attribute> attribute, double baseValue, Collection<AttributeModifier> modifiers) {
        double sumValue = baseValue;

        for(AttributeModifier additive : modifiers.stream().filter(mod -> mod.operation().equals(AttributeModifier.Operation.ADD_VALUE)).toList()) {
            sumValue += additive.amount();
        }

        double productValue = sumValue;

        for(AttributeModifier baseMultiplicative : modifiers.stream().filter(mod -> mod.operation().equals(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)).toList()) {
            productValue += sumValue * baseMultiplicative.amount();
        }

        for(AttributeModifier totalMultiplicative : modifiers.stream().filter(mod -> mod.operation().equals(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)).toList()) {
            productValue *= 1.0D + totalMultiplicative.amount();
        }

        return attribute.value().sanitizeValue(productValue);
    }

    /**
     * The yaw an attacker's weapon hitbox should be oriented by.
     *
     * <p>Better Combat orients a player's swing by {@link Entity#getYRot()}, which for a player is exactly where
     * they are aiming. For a {@link Mob} it is not: {@code yRot} is only steered by {@code MoveControl} while the
     * mob is actively walking somewhere ({@code BodyRotationControl#clientTick} copies {@code yRot} into
     * {@code yBodyRot} only on the {@code isMoving()} branch). A mob that closes the distance and then stands
     * still keeps a stale {@code yRot} pointing wherever it last walked, while its head - and the body you
     * actually see rendered, {@code yBodyRot} - keep tracking the target.
     *
     * <p>Orienting the attack hitbox by {@code yRot} therefore aims the swing off to one side for any stationary
     * mob, so it visibly faces its target and still never connects. Aim by head yaw instead, which is both what
     * the mob is looking at and what the player sees it facing.
     */
    public static float getAttackYRot(LivingEntity attacker) {
        return attacker instanceof Mob ? attacker.getYHeadRot() : attacker.getYRot();
    }

    public static boolean isWithinAttackRange(LivingEntity mob, Entity target, WeaponAttributes.Attack attack, double attackRange) {
        Vec3 origin = MobTargetFinder.getInitialTracingPoint(mob);
        if (!MobAttackRangeExtensions.sources().isEmpty()) {
            attackRange = MobTargetFinder.applyAttackRangeModifiers(mob, attackRange);
        }
        // scale the attack range by the config multiplier, as this method is used for starting attacks, not damage application
        attackRange *= BetterMobCombat.getServerConfig().mob_begin_attack_range_multiplier;

        boolean isSpinAttack = attack.angle() > 180.0;
        Vec3 size = WeaponHitBoxes.createHitbox(attack.hitbox(), attackRange, isSpinAttack);
        OrientedBoundingBox obb = new OrientedBoundingBox(origin, size, mob.getXRot(), getAttackYRot(mob));
        if (!isSpinAttack) {
            obb = obb.offsetAlongAxisZ(size.z / 2.0);
        }

        obb.updateVertex();
        return (new MobTargetFinder.CollisionFilter(obb))
                .and(new MobTargetFinder.RadialFilter(origin, obb.axisZ, attackRange, attack.angle()))
                .test(target, mob);
    }

    public static boolean isAttackReady(Mob mob) {
        return !((MobAttackWindup) mob).bettermobcombat$hasDelayedUpswing() && ((MobAttackWindup) mob).bettermobcombat$getAttackCooldown() <= 0;
    }

    public static void setDelayedUpswing(Mob mob, Runnable runnable){
        ((MobAttackWindup)mob).bettermobcombat$setDelayedUpswing(runnable);
    }
}
