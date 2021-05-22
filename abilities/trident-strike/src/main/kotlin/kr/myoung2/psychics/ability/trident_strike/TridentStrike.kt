package  kr.myoung2.psychics.ability.trident_strike

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.item.PsychicItem
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.event.EntityProvider
import com.github.noonmaru.tap.event.TargetEntity
import org.bukkit.ChatColor
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Name("Trident Strike")
class TridentStrikeConcept: AbilityConcept() {

    @Config
    var tridentStrikeExtraDamage:Double = 0.0

    init {
        displayName = "Trident Strike"
        levelRequirement = 10
        cost = 20.0
        description = listOf("삼지창으로 블록이나 엔티티를 맞추었을때, 하늘에서 번개가 5번 칩니다.",
        "20의 마나를 소모합니다.",
        "기본템은 삼지창(충절)")
    }
}

class TridentStrike: Ability<TridentStrikeConcept>(), Runnable {

    companion object {
        val tridentName = "${ChatColor.RED}뇌격${ChatColor.RESET}의 ${ChatColor.AQUA}삼지창"
        val trident = ItemStack(Material.TRIDENT).also {
            it.itemMeta = it.itemMeta.let { meta ->
                meta.addEnchant(Enchantment.LOYALTY, 3, false)
                meta.setDisplayName(tridentName)
                meta.isUnbreakable = true
                meta.lore = listOf(PsychicItem.boundTag)
                meta.isPsychicbound
                meta
            }
        }
    }

    override fun onEnable() {
        psychic.registerEvents(TridentStrikeListener())
        psychic.runTaskTimer(this, 0L, 1L)
        psychic.esper.player.inventory.addItem(trident)

    }
    override fun run() {
        if (psychic.esper.player.inventory.itemInMainHand == trident) {
            psychic.esper.player.addPotionEffect(PotionEffect(PotionEffectType.SPEED,200,3))
            psychic.esper.player.addPotionEffect(PotionEffect(PotionEffectType.DOLPHINS_GRACE,200,3))

        }
        else {
            psychic.esper.player.removePotionEffect(PotionEffectType.SPEED)
            psychic.esper.player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
        }
    }


    inner class TridentStrikeListener() : Listener {

        var isNeedToGet:Boolean = false

        @EventHandler
        fun onPlayerInteract(event: PlayerInteractEvent) {
            val action = event.action
            when (action) {
                Action.LEFT_CLICK_AIR -> {
                }
                Action.LEFT_CLICK_BLOCK -> {
                }
                Action.RIGHT_CLICK_AIR -> {

                }
                Action.RIGHT_CLICK_BLOCK -> {
                }
                Action.PHYSICAL -> {
                }
                else -> return
            }
        }

        @EventHandler
        @TargetEntity(ProjectileHitProvider::class)
        fun onProjectileHit(event: ProjectileHitEvent) {
            val location = event.entity.location
            val world = event.entity.world
            val player = event.entity.shooter as Player
            if (event.entity !is Trident) return
            if (event.hitEntity != null)
                (event.hitEntity as LivingEntity).damage(concept.tridentStrikeExtraDamage,player)
            val result = test()
            if (result != TestResult.SUCCESS) {
                player.sendActionBar(result.getMessage(this@TridentStrike))
                return
            }


            psychic.consumeMana(concept.cost)
            world.strikeLightning(location)
            world.strikeLightning(location)
            world.strikeLightning(location)
            world.strikeLightning(location)
            world.strikeLightning(location)
        }

        @EventHandler
        fun onPlayerDeath(event: PlayerDeathEvent) {
            if (!event.entity.world.getGameRuleValue(GameRule.KEEP_INVENTORY)!!) {
                if (event.drops.contains(trident)) {
                    event.drops.remove(trident)
                    isNeedToGet = true
                }
            }

        }

        @EventHandler
        fun onPlayerRespawn(event:PlayerRespawnEvent) {
            if (isNeedToGet) {
                event.player.inventory.addItem(trident)
                isNeedToGet = false
            }
        }



    }
}
class ProjectileHitProvider : EntityProvider<ProjectileHitEvent> {
    override fun getFrom(event: ProjectileHitEvent): Entity {
        return if (event.entity.shooter is Player) event.entity.shooter as Player
        else event.entity.shooter as Entity
    }
}

class ProjectileLaunchProvider : EntityProvider<ProjectileLaunchEvent> {
    override fun getFrom(event: ProjectileLaunchEvent): Entity {
        return if (event.entity.shooter is Player) event.entity.shooter as Player
        else event.entity.shooter as Entity
    }
}