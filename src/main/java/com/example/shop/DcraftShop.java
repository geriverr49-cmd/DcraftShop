package com.example.shop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DcraftShop extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ = null;
    private Object ppAPI = null;
    
    private final Map<UUID, String> AKTIF_KATEGORI = new HashMap<>();
    private final Map<UUID, String> AKTIF_ESYA = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Vault Bağlantısı (Para)
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        }

        // PlayerPoints Bağlantısı (Puan)
        Plugin ppPlugin = getServer().getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin != null && ppPlugin.isEnabled()) {
            try { ppAPI = ppPlugin.getClass().getMethod("getAPI").invoke(ppPlugin); } catch (Exception ignored) {}
        }

        if (getCommand("market") != null) getCommand("market").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DcraftShop aktif!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        anaMenuAc(p);
        return true;
    }

    // --- 1. ANA MENÜ (KATEGORİLER) ---
    private void anaMenuAc(Player p) {
        String baslik = getConfig().getString("menu-baslik", "§8§l» §6§lMARKET §8§l«");
        Inventory inv = Bukkit.createInventory(null, 45, baslik);

        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < 45; i++) inv.setItem(i, cam);

        ConfigurationSection kategoriler = getConfig().getConfigurationSection("kategoriler");
        if (kategoriler != null) {
            for (String key : kategoriler.getKeys(false)) {
                ConfigurationSection k = kategoriler.getConfigurationSection(key);
                if (k == null) continue;

                Material mat = Material.matchMaterial(k.getString("material", "STONE"));
                if (mat == null) mat = Material.STONE;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(k.getString("isim", "§eKategori"));
                    meta.setLore(Arrays.asList("§7İçeriği görmek için tıkla!", "§8ID: " + key));
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                inv.setItem(k.getInt("slot", 0), item);
            }
        }

        ItemStack kapat = new ItemStack(Material.BARRIER);
        ItemMeta kMeta = kapat.getItemMeta();
        if (kMeta != null) { kMeta.setDisplayName("§c§lKapat"); kapat.setItemMeta(kMeta); }
        inv.setItem(40, kapat);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    // --- 2. KATEGORİ İÇİ (EŞYALAR) ---
    private void kategoriMenuAc(Player p, String katId) {
        ConfigurationSection k = getConfig().getConfigurationSection("kategoriler." + katId);
        if (k == null) return;

        AKTIF_KATEGORI.put(p.getUniqueId(), katId);

        String baslik = "§8§l» §6§lMARKET - " + ChatColor.stripColor(k.getString("isim", "Kategori")) + " §8§l«";
        Inventory inv = Bukkit.createInventory(null, 54, baslik);

        // Sadece alt kısmı camla doldur (Daha temiz görünüm)
        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 45; i < 54; i++) inv.setItem(i, cam);

        ConfigurationSection esyalar = k.getConfigurationSection("esyalar");
        int slot = 10; // Ortadan başla
        if (esyalar != null) {
            for (String key : esyalar.getKeys(false)) {
                ConfigurationSection e = esyalar.getConfigurationSection(key);
                if (e == null) continue;

                Material mat = Material.matchMaterial(e.getString("material", "STONE"));
                if (mat == null) mat = Material.STONE;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(e.getString("isim", "§eEşya"));
                    List<String> lore = new ArrayList<>();
                    lore.add("§8§m-----------------------");
                    
                    String birim = k.getString("birim", "para");
                    String birimIsmi = birim.equals("para") ? "TL" : "Dcraft Puanı";
                    
                    lore.add("§7Alış Fiyatı: §a" + e.getInt("alis") + " " + birimIsmi);
                    lore.add("§7Satış Fiyatı: §c" + e.getInt("satis") + " " + birimIsmi);
                    lore.add("§8§m-----------------------");
                    lore.add("§aAlmak veya Satmak için tıkla!");
                    lore.add("§8ID: " + key);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
            }
        }

        ItemStack geri = new ItemStack(Material.ARROW);
        ItemMeta gMeta = geri.getItemMeta();
        if (gMeta != null) { gMeta.setDisplayName("§c§lGeri Dön"); geri.setItemMeta(gMeta); }
        inv.setItem(49, geri);

        p.openInventory(inv);
    }

    // --- 3. MİKTAR SEÇİM MENÜSÜ ---
    private void islemMenuAc(Player p, String katId, String esyaId) {
        ConfigurationSection e = getConfig().getConfigurationSection("kategoriler." + katId + ".esyalar." + esyaId);
        if (e == null) return;
        
        ConfigurationSection k = getConfig().getConfigurationSection("kategoriler." + katId);
        String birim = k.getString("birim", "para");
        String birimIsmi = birim.equals("para") ? "TL" : "Puan";

        AKTIF_ESYA.put(p.getUniqueId(), esyaId);

        String baslik = "§8§l» §6§lAL / SAT §8§l«";
        Inventory inv = Bukkit.createInventory(null, 54, baslik);

        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < 54; i++) inv.setItem(i, cam);

        Material mat = Material.matchMaterial(e.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String esyaIsim = e.getString("isim");
        int alis = e.getInt("alis");
        int satis = e.getInt("satis");

        // --- ORTADAKİ EŞYA ---
        ItemStack ortaEsya = new ItemStack(mat);
        ItemMeta oMeta = ortaEsya.getItemMeta();
        if (oMeta != null) {
            oMeta.setDisplayName(esyaIsim);
            oMeta.setLore(Arrays.asList(
                "§8§m-----------------------",
                "§7Alış Fiyatı: §a" + alis + " " + birimIsmi,
                "§7Satış Fiyatı: §c" + satis + " " + birimIsmi,
                "§8§m-----------------------",
                "§7Aşağıdaki camlardan birini seç."
            ));
            oMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            oMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            ortaEsya.setItemMeta(oMeta);
        }
        inv.setItem(22, ortaEsya);

        // --- ALIŞ BUTONLARI (YEŞİL CAM) ---
        inv.setItem(29, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l1 TANE AL", "§7Fiyat: §a" + alis + " " + birimIsmi, "ALIS:1"));
        inv.setItem(30, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l10 TANE AL", "§7Fiyat: §a" + (alis * 10) + " " + birimIsmi, "ALIS:10"));
        inv.setItem(31, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l32 TANE AL", "§7Fiyat: §a" + (alis * 32) + " " + birimIsmi, "ALIS:32"));
        inv.setItem(32, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l64 TANE AL", "§7Fiyat: §a" + (alis * 64) + " " + birimIsmi, "ALIS:64"));
        inv.setItem(33, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§lPARAN KADAR AL", "§7Fiyat: §aHesaplanacak", "ALIS:HEPSI"));

        // --- SATIŞ BUTONLARI (KIRMIZI CAM) ---
        inv.setItem(38, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l1 TANE SAT", "§7Kazanç: §c" + satis + " " + birimIsmi, "SATIS:1"));
        inv.setItem(39, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l10 TANE SAT", "§7Kazanç: §c" + (satis * 10) + " " + birimIsmi, "SATIS:10"));
        inv.setItem(40, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l32 TANE SAT", "§7Kazanç: §c" + (satis * 32) + " " + birimIsmi, "SATIS:32"));
        inv.setItem(41, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l64 TANE SAT", "§7Kazanç: §c" + (satis * 64) + " " + birimIsmi, "SATIS:64"));
        inv.setItem(42, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§lHEPSİNİ SAT", "§7Kazanç: §cHesaplanacak", "SATIS:HEPSI"));

        inv.setItem(4, olusturButon(Material.PAPER, "§e§lBilgilendirme", "§7Yeşil Camlar: §aSatın Al\n§7Kırmızı Camlar: §cElindekini Sat", null));
        inv.setItem(49, olusturButon(Material.ARROW, "§c§lGeri Dön", "§7Kategoriye dön.", null));

        p.openInventory(inv);
    }

    private ItemStack olusturButon(Material mat, String isim, String aciklama, String tag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(isim);
            List<String> lore = new ArrayList<>();
            lore.add(aciklama);
            if (tag != null) lore.add("§8ID: " + tag);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (title.equals(getConfig().getString("menu-baslik", "§8§l» §6§lMARKET §8§l«"))) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER) { p.closeInventory(); return; }
            if (clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null) {
                for (String s : clicked.getItemMeta().getLore()) {
                    if (s.startsWith("§8ID: ")) {
                        kategoriMenuAc(p, s.replace("§8ID: ", ""));
                        break;
                    }
                }
            }
            return;
        }

        if (title.startsWith("§8§l» §6§lMARKET - ")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER) { p.closeInventory(); return; }
            if (clicked.getType() == Material.ARROW) { anaMenuAc(p); return; }

            if (clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null) {
                String katId = AKTIF_KATEGORI.get(p.getUniqueId());
                for (String s : clicked.getItemMeta().getLore()) {
                    if (s.startsWith("§8ID: ")) {
                        islemMenuAc(p, katId, s.replace("§8ID: ", ""));
                        break;
                    }
                }
            }
            return;
        }

        if (title.equals("§8§l» §6§lAL / SAT §8§l«")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.ARROW) { 
                kategoriMenuAc(p, AKTIF_KATEGORI.get(p.getUniqueId())); 
                return; 
            }

            if (clicked.hasItemMeta() && clicked.getItemMeta().getLore() != null) {
                String tag = null;
                for (String s : clicked.getItemMeta().getLore()) {
                    if (s.startsWith("§8ID: ")) { tag = s.replace("§8ID: ", ""); break; }
                }
                
                if (tag == null || tag.equals("null")) return;

                String katId = AKTIF_KATEGORI.get(p.getUniqueId());
                String esyaId = AKTIF_ESYA.get(p.getUniqueId());

                if (tag.startsWith("ALIS:")) {
                    islemYap(p, katId, esyaId, "AL", tag.replace("ALIS:", ""));
                } else if (tag.startsWith("SATIS:")) {
                    islemYap(p, katId, esyaId, "SAT", tag.replace("SATIS:", ""));
                }
            }
            return;
        }
    }

    // --- İŞLEM YAPMA (PARA VEYA PUAN) ---
    private void islemYap(Player p, String katId, String esyaId, String tur, String miktarStr) {
        ConfigurationSection e = getConfig().getConfigurationSection("kategoriler." + katId + ".esyalar." + esyaId);
        if (e == null) return;
        
        ConfigurationSection k = getConfig().getConfigurationSection("kategoriler." + katId);
        String birim = k.getString("birim", "para");

        Material mat = Material.matchMaterial(e.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        int birimFiyat = tur.equals("AL") ? e.getInt("alis") : e.getInt("satis");
        int miktar = 0;

        if (miktarStr.equals("HEPSI")) {
            if (tur.equals("AL")) {
                int bakiye = getBakiye(p, birim);
                miktar = bakiye / birimFiyat;
                if (miktar <= 0) { p.sendMessage("§cYetersiz bakiyen!"); p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); return; }
            } else {
                miktar = envanterdeKacTaneVar(p, mat);
                if (miktar <= 0) { p.sendMessage("§cEnvanterinde satacak eşya yok!"); p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); return; }
            }
        } else {
            miktar = Integer.parseInt(miktarStr);
        }

        int toplamFiyat = birimFiyat * miktar;

        if (tur.equals("AL")) {
            if (getBakiye(p, birim) < toplamFiyat) {
                p.sendMessage("§cYetersiz bakiyen! Gerekli: §e" + toplamFiyat);
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            paraCek(p, birim, toplamFiyat);
            
            ItemStack item = new ItemStack(mat, miktar);
            HashMap<Integer, ItemStack> kalanlar = p.getInventory().addItem(item);
            for (ItemStack kalan : kalanlar.values()) p.getWorld().dropItemNaturally(p.getLocation(), kalan);

            p.sendMessage("§a§l✔ §aBaşarıyla §e" + miktar + "x " + ChatColor.stripColor(e.getString("isim")) + " §asatın aldın!");
            p.sendMessage("§7Ödenen: §e" + toplamFiyat + " " + (birim.equals("para") ? "TL" : "Puan"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        } else {
            if (envanterdeKacTaneVar(p, mat) < miktar) {
                p.sendMessage("§cEnvanterinde yeterince eşya yok!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            envanterdenSil(p, mat, miktar);
            paraVer(p, birim, toplamFiyat);

            p.sendMessage("§a§l✔ §aBaşarıyla §e" + miktar + "x " + ChatColor.stripColor(e.getString("isim")) + " §asattın!");
            p.sendMessage("§7Kazanılan: §e" + toplamFiyat + " " + (birim.equals("para") ? "TL" : "Puan"));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }

        p.closeInventory();
    }

    // --- BAKİYE İŞLEMLERİ (PARA VEYA PUAN) ---
    private int getBakiye(Player p, String birim) {
        if (birim.equals("para")) {
            return econ != null ? (int) econ.getBalance(p) : 0;
        } else {
            if (ppAPI == null) return 0;
            try { return (int) ppAPI.getClass().getMethod("look", UUID.class).invoke(ppAPI, p.getUniqueId()); } catch (Exception ex) { return 0; }
        }
    }

    private void paraCek(Player p, String birim, int miktar) {
        if (birim.equals("para")) {
            if (econ != null) econ.withdrawPlayer(p, miktar);
        } else {
            if (ppAPI != null) {
                try { ppAPI.getClass().getMethod("take", UUID.class, int.class).invoke(ppAPI, p.getUniqueId(), miktar); } catch (Exception ignored) {}
            }
        }
    }

    private void paraVer(Player p, String birim, int miktar) {
        if (birim.equals("para")) {
            if (econ != null) econ.depositPlayer(p, miktar);
        } else {
            if (ppAPI != null) {
                try { ppAPI.getClass().getMethod("give", UUID.class, int.class).invoke(ppAPI, p.getUniqueId(), miktar); } catch (Exception ignored) {}
            }
        }
    }

    private int envanterdeKacTaneVar(Player p, Material mat) {
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == mat) count += item.getAmount();
        }
        return count;
    }

    private void envanterdenSil(Player p, Material mat, int miktar) {
        int kalan = miktar;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= kalan) { kalan -= item.getAmount(); p.getInventory().setItem(i, null); } 
                else { item.setAmount(item.getAmount() - kalan); kalan = 0; }
                if (kalan <= 0) break;
            }
        }
    }
    }
