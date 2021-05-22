package kr.myoung2.psychics.ability.stealth
import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.Esper
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.protocol.Packet
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Name("Stealth")
class StealthConcept: AbilityConcept() {

    @Config
    var dummy:Boolean = true

    init {
        displayName = "Stealth"
        levelRequirement = 5
        description = listOf()
        cost=25.0
    }
}

class Stealth: Ability<StealthConcept>(), Listener, Runnable {
    override fun onEnable() {
        psychic.registerEvents(this)
        psychic.runTaskTimer(this, 0L, 1L)
    }

    var fakeEntity:FakeEntity? = null

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        val result = test()
        if (result != TestResult.SUCCESS) {
            event.player.sendActionBar(result.getMessage(this@Stealth))
            return
        }
        if (event.isSneaking) {

            psychic.consumeMana(concept.cost)
            if (concept.dummy) {
                fakeEntity = psychic.spawnFakeEntity(event.player.location,ArmorStand::class.java).apply {
                    updateEquipment {
                        helmet = ItemStack(Material.PLAYER_HEAD).apply {
                            itemMeta = (itemMeta as SkullMeta).apply {
                                playerProfile = event.player.playerProfile
                                owningPlayer = event.player
                            }
                        }

                    }
                    updateMetadata<ArmorStand> {
                        setArms(true)
                        setBasePlate(false)
                    }

                }
            }
            event.player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY,200,1,true,false,true))
        }
        else {
            if (concept.dummy) {
                if (fakeEntity != null) {
                    fakeEntity.let { fe ->
                        fe?.remove()
                        fakeEntity = null
                    }
                }
            }
            event.player.removePotionEffect(PotionEffectType.INVISIBILITY)
        }
    }

    override fun run() {
    }
}