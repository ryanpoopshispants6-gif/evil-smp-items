package com.evilsmp.reborn;

import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.util.*;

public final class RebornPlugin extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey itemKey, healthKey;
    private final Map<UUID, Map<String,Long>> cooldown = new HashMap<>();
    private final Map<UUID,Long> nullified = new HashMap<>(), restricted = new HashMap<>(), axeActive = new HashMap<>();
    private final Set<UUID> healthModified = new HashSet<>();
    private static final long FIVE_MIN = 300000L, TWO_MIN = 120000L;
    private static final Set<PotionEffectType> POSITIVE = Set.of(PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.STRENGTH, PotionEffectType.JUMP_BOOST, PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING, PotionEffectType.INVISIBILITY, PotionEffectType.NIGHT_VISION, PotionEffectType.HEALTH_BOOST, PotionEffectType.ABSORPTION, PotionEffectType.SATURATION, PotionEffectType.LUCK, PotionEffectType.SLOW_FALLING, PotionEffectType.CONDUIT_POWER, PotionEffectType.DOLPHINS_GRACE, PotionEffectType.HERO_OF_THE_VILLAGE);

    @Override public void onEnable() {
        itemKey = new NamespacedKey(this,"ability_item"); healthKey = new NamespacedKey(this,"nullified_health");
        Bukkit.getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("evilsmpitems")).setExecutor(this);
        Objects.requireNonNull(getCommand("evilsmpitems")).setTabCompleter(this);
        // Re-check frequently because DragonEggBuffs may reapply buffs or alter max-health attributes.
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 2L);
    }
    @Override public void onDisable() {
        for (Player p:Bukkit.getOnlinePlayers()) removeHealthLimit(p);
    }
    private long now(){return System.currentTimeMillis();}
    private boolean active(Map<UUID,Long> map, Player p){return map.getOrDefault(p.getUniqueId(),0L)>now();}
    private boolean cooling(Player p,String type){return cooldown.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(type,0L)>now();}
    private void startCooldown(Player p,String type, Material material,long ms){
        cooldown.computeIfAbsent(p.getUniqueId(), k->new HashMap<>()).put(type,now()+ms);
        // Minecraft grey radial cooldown overlay, including on the actual item.
        p.setCooldown(material,(int)(ms/50));
    }
    private String itemType(ItemStack item){
        if(item==null||!item.hasItemMeta())return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);
    }
    private ItemStack make(String type){
        Material m; String name; List<String> lore;
        switch(type){
            case "tickle" -> {m=Material.FEATHER;name="&b&lTickle Feather";lore=List.of("&7Hit: Slow Falling for 3 minutes","&eCooldown: 5 minutes");}
            case "nullifier" -> {m=Material.OBSIDIAN;name="&5&lNullifier";lore=List.of("&7Hit: suppress positive effects","&7Weakness II + Mining Fatigue I","&7Max 10 hearts for 5 minutes","&eCooldown: 5 minutes");}
            case "armourbreaker" -> {m=Material.NETHERITE_AXE;name="&c&lArmour Breaker";lore=List.of("&7Shift + left-click to activate","&7For 5 seconds, each hit deals","&c50 durability to EACH armour piece","&eCooldown: 2 minutes");}
            case "restrictor" -> {m=Material.COMPARATOR;name="&6&lRestrictor";lore=List.of("&7Hit: block Elytra, spears and XP bottles","&7Duration: 5 minutes","&eCooldown: 5 minutes");}
            default -> {return null;}
        }
        ItemStack item=new ItemStack(m); ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',name));
        meta.setLore(lore.stream().map(s->ChatColor.translateAlternateColorCodes('&',s)).toList());
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,type);
        item.setItemMeta(meta);return item;
    }
    @EventHandler(ignoreCancelled=true) public void onHit(EntityDamageByEntityEvent e){
        if(!(e.getDamager() instanceof Player a)||!(e.getEntity() instanceof Player t))return;
        String type=itemType(a.getInventory().getItemInMainHand());if(type==null)return;
        if(type.equals("armourbreaker")){
            if(active(axeActive,a)) damageArmour(t);
            return;
        }
        if(cooling(a,type))return;
        switch(type){
            case "tickle" -> {t.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,3600,0));startCooldown(a,type,Material.FEATHER,FIVE_MIN);}
            case "nullifier" -> {nullified.put(t.getUniqueId(),now()+FIVE_MIN);suppress(t);startCooldown(a,type,Material.OBSIDIAN,FIVE_MIN);t.sendMessage(ChatColor.DARK_PURPLE+"Nullified for 5 minutes!");}
            case "restrictor" -> {restricted.put(t.getUniqueId(),now()+FIVE_MIN);if(t.isGliding())t.setGliding(false);startCooldown(a,type,Material.COMPARATOR,FIVE_MIN);t.sendMessage(ChatColor.RED+"Elytra, spears and XP bottles restricted for 5 minutes!");}
        }
    }
    @EventHandler(ignoreCancelled=true) public void activateAxe(PlayerInteractEvent e){
        Player p=e.getPlayer();if(!p.isSneaking())return;
        if(e.getAction()!=Action.LEFT_CLICK_AIR&&e.getAction()!=Action.LEFT_CLICK_BLOCK)return;
        if(!"armourbreaker".equals(itemType(p.getInventory().getItemInMainHand())))return;
        if(cooling(p,"armourbreaker"))return;
        axeActive.put(p.getUniqueId(),now()+5000L);
        startCooldown(p,"armourbreaker",Material.NETHERITE_AXE,TWO_MIN);
        p.sendMessage(ChatColor.RED+"Armour Breaker ACTIVE for 5 seconds!");
    }
    private void damageArmour(Player target){
        ItemStack[] pieces=target.getInventory().getArmorContents();
        for(int i=0;i<pieces.length;i++){
            ItemStack piece=pieces[i];if(piece==null||piece.getType().isAir())continue;
            if(!(piece.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d))continue;
            int max=piece.getType().getMaxDurability();if(max<=0)continue;
            int updated=d.getDamage()+50;
            if(updated>=max){pieces[i]=null;target.getWorld().playSound(target.getLocation(),Sound.ENTITY_ITEM_BREAK,1f,1f);}
            else {d.setDamage(updated);piece.setItemMeta(d);pieces[i]=piece;}
        }
        target.getInventory().setArmorContents(pieces);
    }
    private void tick(){
        for(Player p:Bukkit.getOnlinePlayers()){
            if(active(nullified,p))suppress(p);
            else if(healthModified.contains(p.getUniqueId())){removeHealthLimit(p);nullified.remove(p.getUniqueId());p.sendMessage(ChatColor.GREEN+"Nullifier has worn off.");}
            if(active(restricted,p)&&p.isGliding())p.setGliding(false);
        }
    }
    private void suppress(Player p){
        for(PotionEffect effect:p.getActivePotionEffects())if(POSITIVE.contains(effect.getType()))p.removePotionEffect(effect.getType());
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,60,1,false,false,true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,60,0,false,false,true));
        AttributeInstance a=p.getAttribute(Attribute.MAX_HEALTH);if(a==null)return;
        // Adjust our own modifier to offset the full effective max, including DragonEggBuffs modifiers.
        a.removeModifier(healthKey);
        double excess=Math.max(0,a.getValue()-20.0);
        if(excess>0)a.addModifier(new AttributeModifier(healthKey,-excess,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.ANY));
        healthModified.add(p.getUniqueId());
        if(p.getHealth()>20)p.setHealth(20);
    }
    private void removeHealthLimit(Player p){
        AttributeInstance a=p.getAttribute(Attribute.MAX_HEALTH);if(a!=null)a.removeModifier(healthKey);
        healthModified.remove(p.getUniqueId());
    }
    private boolean spear(ItemStack i){return i!=null&&(i.getType().name().equals("SPEAR")||i.getType().name().endsWith("_SPEAR"));}
    @EventHandler(ignoreCancelled=true) public void restrictInteract(PlayerInteractEvent e){
        if(!active(restricted,e.getPlayer()))return;
        if(spear(e.getItem())||(e.getItem()!=null&&e.getItem().getType()==Material.EXPERIENCE_BOTTLE))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void restrictSpearDamage(EntityDamageByEntityEvent e){
        if(e.getDamager() instanceof Player p&&active(restricted,p)&&spear(p.getInventory().getItemInMainHand()))e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled=true) public void restrictGlide(EntityToggleGlideEvent e){
        if(e.getEntity() instanceof Player p&&e.isGliding()&&active(restricted,p))e.setCancelled(true);
    }
    @EventHandler public void onJoin(PlayerJoinEvent e){
        // Cooldowns are re-applied after reconnect within the same server session.
        Player p=e.getPlayer();Map<String,Long> cds=cooldown.get(p.getUniqueId());if(cds==null)return;
        for(var entry:cds.entrySet()){
            long left=entry.getValue()-now();if(left<=0)continue;
            ItemStack sample=make(entry.getKey());if(sample!=null)p.setCooldown(sample.getType(),(int)(left/50));
        }
    }
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] args){
        if(!s.hasPermission("evilsmpabilities.admin")){s.sendMessage("No permission.");return true;}
        if(args.length!=3||!args[0].equalsIgnoreCase("give")){s.sendMessage("/evilsmpitems give <player> <tickle|nullifier|armourbreaker|restrictor>");return true;}
        Player p=Bukkit.getPlayerExact(args[1]);if(p==null){s.sendMessage("Player not online.");return true;}
        ItemStack item=make(args[2].toLowerCase(Locale.ROOT));if(item==null){s.sendMessage("Unknown item.");return true;}
        p.getInventory().addItem(item).values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));s.sendMessage("Given item to "+p.getName());return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){
        if(a.length==1)return List.of("give");if(a.length==2)return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();if(a.length==3)return List.of("tickle","nullifier","armourbreaker","restrictor");return List.of();
    }
}
