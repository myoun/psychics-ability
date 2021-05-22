package kr.myoung2.psychics.ability.stone_strike

import com.github.noonmaru.psychics.*
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.effect.playFirework
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.math.copy
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.trail.trail
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BoundingBox
import kotlin.random.Random.Default.nextFloat

@Name("Stone Strike")
class StoneStrikeConcept: AbilityConcept() {

    @Config
    var stoneTicks = 100

    @Config
    var stoneSize = 1.0

    @Config
    var stoneFragmentRadius = 3.0

    @Config
    var stoneKnockback = 1.0

    @Config
    var stoneAcceleration = 0.02

    @Config
    var stoneInitSpeed = 1.0

    init {
        displayName = "Stone Strike"
        cost = 0.0
        wand = ItemStack(Material.STONE)
        castingBarColor = BarColor.WHITE
        castingTicks = 20
        damage = Damage(DamageType.RANGED,EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 2.0))
        range = 50.0
    }
}

class StoneStrike: ActiveAbility<StoneStrikeConcept>(), Listener, Runnable {

    private var stone:Stone? =null

    override fun onEnable() {
        psychic.registerEvents(this)
        //psychic.runTaskTimer(this, 0L, 1L)
    }

    override fun onAttach() {
        super.onAttach()
    }

    override fun tryCast(
        event: PlayerEvent,
        action: WandAction,
        castingTicks: Long,
        cost: Double,
        targeter: (() -> Any?)?
    ): TestResult {
        val ret = super.tryCast(event, action, castingTicks, cost, targeter)
        if (ret == TestResult.SUCCESS) stone = Stone(esper.player.eyeLocation)
        return ret
    }

    override fun onChannel(channel: Channel) {
        stone?.run {
            updateLocation(if (channel.remainingTicks > concept.castingTicks -3) - 60.0 else 0.0)
        }
    }

    override fun onInterrupt(channel: Channel) {
        stone?.remove()
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        stone.let { stone ->
            val projectile = StoneProjectile(stone!!)
            psychic.launchProjectile(stone.location,projectile)
            projectile.velocity = stone.location.direction.multiply(concept.stoneInitSpeed)
            this.stone = null
        }
    }

    override fun run() {
        TODO("Not yet implemented")
    }

    inner class Stone(internal val location: Location) {
        private var armorStand:FakeEntity? = null

        init {
            armorStand = psychic.spawnFakeEntity(location,ArmorStand::class.java).apply {
                updateMetadata<ArmorStand> {
                    isMarker = true
                    isVisible = false
                }
                updateEquipment {
                    helmet = ItemStack(Material.COBBLESTONE)
                }
            }

        }

        fun updateLocation(offsetY: Double = 0.0, newLoc:Location = location) {
            val x = newLoc.x
            val y = newLoc.y
            val z = newLoc.z

            location.apply {
                world = newLoc.world
                this.x = x
                this.y = y
                this.z = z
            }

            val loc = newLoc.clone()
            armorStand?.moveTo(loc.apply {
                copy(newLoc)
                pitch = 0.0F
            })
        }

        fun remove() {
            armorStand.let {
                it?.remove()
                null
            }
        }
    }

    inner class StoneProjectile(private val stone: Stone) : PsychicProjectile(concept.stoneTicks,concept.range) {

        override fun onMove(movement: Movement) {
            stone.updateLocation(0.0,movement.to)
        }

        override fun onTrail(trail: Trail) {
            trail.velocity?.let { velocity ->
                val from = trail.from
                val world = from.world
                val length = velocity.normalizeAndLength()
                val filter=  TargetFilter(esper.player)

                world.rayTrace(
                    from,
                    velocity,
                    length,
                    FluidCollisionMode.NEVER,
                    true,
                    concept.stoneSize,
                    filter
                )?.let { result ->
                    remove()
                    val hitPosition = result.hitPosition
                    val hitLocation = hitPosition.toLocation(world)

                    val firework =
                        FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.GRAY).build()
                    world.playFirework(hitLocation,firework)

                    concept.damage?.let { damage ->
                        val radius = concept.stoneFragmentRadius
                        val box = BoundingBox.of(hitPosition,radius,radius,radius)

                        for (entity in world.getNearbyEntities(box,filter)) {
                            if (entity is LivingEntity) {
                                entity.psychicDamage(damage,hitLocation,concept.stoneKnockback)
                            }
                        }
                    }
                }

                val to = trail.to
                trail(from,to,0.25) {w,x,y,z ->
                    w.spawnParticle(
                        Particle.CRIT_MAGIC,
                        x,y,z,
                        5,
                        0.1,0.1,0.1,
                        0.25,null,true
                    )
                }

                to.world.playSound(to,Sound.BLOCK_STONE_BREAK,SoundCategory.MASTER,0.25F,1.8F+nextFloat()*0.2F)
            }
        }

        override fun onPostUpdate() {
            velocity = velocity.multiply(1.0 + concept.stoneAcceleration)
        }

        override fun onRemove() {
            stone.remove()
        }

    }




}

