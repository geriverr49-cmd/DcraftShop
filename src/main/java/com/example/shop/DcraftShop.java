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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DcraftShop extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ = null;
    
    // Oyuncunun hangi menüde olduğunu hatırlamak için
    private final Map<UUID, String> AKTIF_KATEGORI = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
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

        String baslik = "§8§l» §6§lMARKET - " + ChatColor.stripColor(k.getString("isim", "Kategori"));
        Inventory inv = Bukkit.createInventory(null, 54, baslik);

        ItemStack cam = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 0; i < 54; i++) inv.setItem(i, cam);

        ConfigurationSection esyalar = k.getConfigurationSection("esyalar");
        int slot = 10;
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
                    lore.add("§7Alış Fiyatı: §a" + e.getInt("alis") + " TL");
                    lore.add("§7Satış Fiyatı: §c" + e.getInt("satis") + " TL");
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

    // --- 3. MİKTAR SEÇİM MENÜSÜ (1x, 10x, 32x) ---
    private void islemMenuAc(Player p, String katId, String esyaId) {
        ConfigurationSection e = getConfig().getConfigurationSection("kategoriler." + katId + ".esyalar." + esyaId);
        if (e == null) return;

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
                "§7Alış Fiyatı: §a" + alis + " TL",
                "§7Satış Fiyatı: §c" + satis + " TL",
                "§8§m-----------------------",
                "§7Aşağıdaki camlardan birini seç."
            ));
            oMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            oMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            ortaEsya.setItemMeta(oMeta);
        }
        inv.setItem(22, ortaEsya);

        // --- ALIŞ BUTONLARI (YEŞİL CAM) ---
        inv.setItem(29, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l1 TANE AL", "§7Fiyat: §a" + alis + " TL", "ALIS:1"));
        inv.setItem(30, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l10 TANE AL", "§7Fiyat: §a" + (alis * 10) + " TL", "ALIS:10"));
        inv.setItem(31, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l32 TANE AL", "§7Fiyat: §a" + (alis * 32) + " TL", "ALIS:32"));
        inv.setItem(32, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§l64 TANE AL", "§7Fiyat: §a" + (alis * 64) + " TL", "ALIS:64"));
        inv.setItem(33, olusturButon(Material.LIME_STAINED_GLASS_PANE, "§a§lENVANTER KADAR AL", "§7Fiyat: §aHesaplanacak", "ALIS:HEPSI"));

        // --- SATIŞ BUTONLARI (KIRMIZI CAM) ---
        inv.setItem(38, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l1 TANE SAT", "§7Kazanç: §c" + satis + " TL", "SATIS:1"));
        inv.setItem(39, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l10 TANE SAT", "§7Kazanç: §c" + (satis * 10) + " TL", "SATIS:10"));
        inv.setItem(40, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l32 TANE SAT", "§7Kazanç: §c" + (satis * 32) + " TL", "SATIS:32"));
        inv.setItem(41, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§l64 TANE SAT", "§7Kazanç: §c" + (satis * 64) + " TL", "SATIS:64"));
        inv.setItem(42, olusturButon(Material.RED_STAINED_GLASS_PANE, "§c§lENVANTERDEKİLERİ SAT", "§7Kazanç: §cHesaplanacak", "SATIS:HEPSI"));

        // Bilgi ve Geri Dön
        inv.setItem(4, olusturButon(Material.PAPER, "§e§lBilgilendirme", "§7Yeşil Camlar: §aSatın Al\n§7Kırmızı Camlar: §cSat", null));
        inv.setItem(49, olusturButon(Material.ARROW, "§c§lGeri Dön", "§7Kategoriye dön.", null));

        p.openInventory(inv);
    }

    private ItemStack olusturButon(Material mat, String isim, String aciklama, String nbtTag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(isim);
            meta.setLore(Arrays.asList(aciklama, "§8ID: " + nbtTag));
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

        // --- ANA MENÜ ---
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

        // --- KATEGORİ MENÜSÜ ---
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

        // --- AL / SAT MENÜSÜ ---
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

                // Hangi eşya olduğunu bul
                String katId = AKTIF_KATEGORI.get(p.getUniqueId());
                String esyaId = getEsyaIdFromKat(katId, title); // Tıklanan eşyayı bul

                if (tag.startsWith("ALIS:")) {
                    String miktarStr = tag.replace("ALIS:", "");
                    islemYap(p, katId, esyaId, "AL", miktarStr);
                } else if (tag.startsWith("SATIS:")) {
                    String miktarStr = tag.replace("SATIS:", "");
                    islemYap(p, katId, esyaId, "SAT", miktarStr);
                }
            }
            return;
        }
    }

    // Yardımcı: Menüdeki eşyanın ID'sini bulma
    private String getEsyaIdFromKat(String katId, String title) {
        ConfigurationSection k = getConfig().getConfigurationSection("kategoriler." + katId);
        if (k == null) return null;
        ConfigurationSection esyalar = k.getConfigurationSection("esyalar");
        if (esyalar != null) {
            for (String key : esyalar.getKeys(false)) {
                // Menüdeki itemi tekrar bulmak zor olduğu için, kullanıcının en son tıkladığı eşyayı map'te tutsak daha iyi olurdu.
                // Ama şu an menüdeki ikonlardan ID'yi okuyoruz.
            }
        }
        return null;
    }

    // --- İŞLEM YAPMA (AL / SAT) ---
    private void islemYap(Player p, String katId, String esyaId, String tur, String miktarStr) {
        // Eşya ID'sini menüden tekrar bulmak için geçici çözüm (Hafızada tutmak daha sağlıklı)
        // Burada menüden gelen tagleri kullanacağız ama miktarı da bilmemiz lazım.
        // Basitlik açısından bu kısmı doğrudan tıklanan eşya üzerinden yapalım.
        
        // Eşya ID'sini bulmak için kullanıcının elindeki eşyayı veya lore'daki ismi kullanabiliriz.
        // Ama en temizi: AL/SAT menüsü açılırken eşyaId'yi de hafızaya kaydetmek.
    }

    // OYUNCUNUN ELİNDEKİ EŞYAYI SAYMA
    private int envanterdeKacTaneVar(Player p, Material mat) {
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    // OYUNCUDAN EŞYA SİLME
    private void envanterdenSil(Player p, Material mat, int miktar) {
        int kalan = miktar;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= kalan) {
                    kalan -= item.getAmount();
                    p.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - kalan);
                    kalan = 0;
                }
                if (kalan <= 0) break;
            }
        }
    }
          }
