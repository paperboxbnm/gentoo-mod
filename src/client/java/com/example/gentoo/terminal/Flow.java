package com.example.gentoo.terminal;

import java.util.ArrayList;
import java.util.List;

/** The whole install, split into the steps you would really type in the Gentoo handbook. */
public final class Flow {

    public static final List<Stage> STAGES = new ArrayList<>();

    private static void s(String expect, String hint, int speed, boolean chroot, String... out) {
        STAGES.add(new Stage(expect, hint, speed, chroot, out));
    }

    static {
        // ---------- live cd ----------
        s("ip\\s+(a|addr)", "ip a", 1, false,
            "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue",
            "    inet 127.0.0.1/8 scope host lo",
            "2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 state UP",
            "    link/ether 52:54:00:3f:1a:88 brd ff:ff:ff:ff:ff:ff",
            "    inet 192.168.1.42/24 brd 192.168.1.255 scope global eth0");

        s("ping\\s+-c\\s*3\\s+gentoo\\.org", "ping -c 3 gentoo.org", 2, false,
            "PING gentoo.org (151.101.1.91) 56(84) bytes of data.",
            "64 bytes from 151.101.1.91: icmp_seq=1 ttl=57 time=24.1 ms",
            "64 bytes from 151.101.1.91: icmp_seq=2 ttl=57 time=23.4 ms",
            "64 bytes from 151.101.1.91: icmp_seq=3 ttl=57 time=25.0 ms",
            "--- gentoo.org ping statistics ---",
            "3 packets transmitted, 3 received, 0% packet loss, time 2003ms");

        // ---------- partitions ----------
        s("parted\\s+(-a\\s+optimal\\s+)?/dev/sda\\s+mklabel\\s+gpt", "parted -a optimal /dev/sda mklabel gpt", 1, false,
            "Information: You may need to update /etc/fstab.");

        s("parted\\s+(-a\\s+optimal\\s+)?/dev/sda\\s+mkpart\\s+primary\\s+fat32\\s+1MiB\\s+513MiB", "parted /dev/sda mkpart primary fat32 1MiB 513MiB", 1, false,
            "Information: You may need to update /etc/fstab.");

        s("parted\\s+(-a\\s+optimal\\s+)?/dev/sda\\s+mkpart\\s+primary\\s+(linux-swap|swap)\\s+513MiB\\s+4608MiB", "parted /dev/sda mkpart primary linux-swap 513MiB 4608MiB", 1, false,
            "Information: You may need to update /etc/fstab.");

        s("parted\\s+(-a\\s+optimal\\s+)?/dev/sda\\s+mkpart\\s+primary\\s+ext4\\s+4608MiB\\s+100%", "parted /dev/sda mkpart primary ext4 4608MiB 100%", 1, false,
            "Information: You may need to update /etc/fstab.");

        s("parted\\s+/dev/sda\\s+set\\s+1\\s+esp\\s+on", "parted /dev/sda set 1 esp on", 1, false,
            "Information: You may need to update /etc/fstab.");

        s("parted\\s+/dev/sda\\s+print", "parted /dev/sda print", 1, false,
            "Model: ATA QEMU HARDDISK (scsi)",
            "Disk /dev/sda: 64.0GB",
            "Number  Start   End     Size    File system     Name     Flags",
            " 1      1049kB  538MB   537MB   fat32           primary  boot, esp",
            " 2      538MB   4948MB  4410MB  linux-swap(v1)  primary",
            " 3      4948MB  64.0GB  59.1GB  ext4            primary");

        // ---------- filesystems ----------
        s("mkfs\\.vfat\\s+(-F\\s*32\\s+)?/dev/sda1", "mkfs.vfat -F32 /dev/sda1", 2, false,
            "mkfs.fat 4.2 (2021-01-31)",
            "/dev/sda1: 523776 sectors, 511 MiB",
            "Heads: 255  Sectors per track: 63");

        s("mkswap\\s+/dev/sda2", "mkswap /dev/sda2", 1, false,
            "Setting up swapspace version 1, size = 3.9 GiB (4193255424 bytes)",
            "no label, UUID=8e3a6c1d-2f77-4a0e-9c21-5b7bd0e4f1aa");

        s("swapon\\s+/dev/sda2", "swapon /dev/sda2", 1, false,
            " * Activating swap /dev/sda2 ... [ ok ]");

        s("mkfs\\.ext4\\s+(-F\\s+)?/dev/sda3", "mkfs.ext4 /dev/sda3", 2, false,
            "mke2fs 1.47.0 (5-Feb-2023)",
            "Creating filesystem with 14438400 4k blocks and 3610112 inodes",
            "Filesystem UUID: 4f1c2e7a-8f2b-4d61-9c33-6a2f0d8e5b17",
            "Superblock backups stored on blocks: 32768, 98304, 163840",
            "Writing inode tables: done",
            "Creating journal (65536 blocks): done",
            "Writing superblocks and filesystem accounting information: done");

        // ---------- mounting ----------
        s("mount\\s+/dev/sda3\\s+/mnt/gentoo", "mount /dev/sda3 /mnt/gentoo", 1, false,
            " * Mounting /dev/sda3 on /mnt/gentoo ... [ ok ]");

        s("mkdir\\s+(-p\\s+)?/mnt/gentoo/boot", "mkdir /mnt/gentoo/boot", 1, false,
            " * /mnt/gentoo/boot created");

        s("mount\\s+/dev/sda1\\s+/mnt/gentoo/boot", "mount /dev/sda1 /mnt/gentoo/boot", 1, false,
            " * Mounting /dev/sda1 on /mnt/gentoo/boot ... [ ok ]");

        s("cd\\s+/mnt/gentoo/?", "cd /mnt/gentoo", 1, false,
            " * now in /mnt/gentoo");

        // ---------- stage3 ----------
        s("tar\\s+x?[a-z]*v?[a-z]*f?\\s+stage3-amd64-openrc\\.tar\\.xz.*", "tar xpvf stage3-amd64-openrc.tar.xz --xattrs-include='*.*' --numeric-owner", 1, false,
            "./bin/",
            "./bin/bash",
            "./etc/",
            "./etc/portage/",
            "./etc/portage/make.conf",
            "./usr/",
            "./usr/bin/",
            "./usr/bin/emerge",
            "./usr/bin/portageq",
            "./var/",
            "./var/db/repos/",
            "./lib64/ld-linux-x86-64.so.2",
            "./lib/systemd/",
            "./stage3-amd64-openrc-20240201T170301Z/",
            "stage3 extracted: 41288 files, 1.2 GiB");

        s("cp\\s+(--dereference\\s+)?/etc/resolv\\.conf\\s+/mnt/gentoo/etc/resolv\\.conf", "cp --dereference /etc/resolv.conf /mnt/gentoo/etc/resolv.conf", 1, false,
            " * resolv.conf copied into the new root");

        // ---------- chroot ----------
        s("chroot\\s+/mnt/gentoo\\s+(/bin/bash)?", "chroot /mnt/gentoo /bin/bash", 2, false,
            " * Entering /mnt/gentoo (bash 5.2.21)",
            " * You are now root inside the new system. Mind your PATH.");

        s("source\\s+/etc/profile", "source /etc/profile", 1, true,
            " * profile sourced, PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");

        // ---------- emerge ----------
        s("emerge-webrsync", "emerge-webrsync", 3, true,
            ">>> Syncing repository 'gentoo' into '/var/db/repos/gentoo'...",
            ">>> Fetching most recent snapshot ...",
            ">>> rsync://rsync.gentoo.org/gentoo-portage",
            "receiving file list ... done",
            ">>> Updating ebuild repository 'gentoo' ...",
            ">>> Syncing repository 'gentoo'... done.",
            "* Fetching gentoo snapshot ... [ ok ]");

        s("emerge\\s+(--ask\\s+)?sys-kernel/gentoo-sources", "emerge --ask sys-kernel/gentoo-sources", 3, true,
            "These are the packages that would be merged, in order:",
            "",
            "Calculating dependencies... done!",
            "[ebuild  N     ] sys-kernel/gentoo-sources-6.6.21  USE=\"-experimental\"",
            "",
            "Would you like to merge these packages? [Yes/No] y",
            ">>> Emerging (1 of 1) sys-kernel/gentoo-sources-6.6.21::gentoo",
            ">>> Downloading 'https://distfiles.gentoo.org/distfiles/gentoo-sources-6.6.21.tar.xz'",
            ">>> Unpacking gentoo-sources-6.6.21.tar.xz to /var/tmp/portage",
            ">>> Installing (1 of 1) sys-kernel/gentoo-sources-6.6.21::gentoo",
            ">>> Jobs: 1 of 1 complete");

        s("emerge\\s+(--ask\\s+)?(sys-kernel/installkernel\\s+)?sys-boot/grub(\\s+sys-kernel/installkernel)?", "emerge --ask sys-kernel/installkernel sys-boot/grub", 3, true,
            "These are the packages that would be merged, in order:",
            "",
            "Calculating dependencies... done!",
            "[ebuild  N     ] sys-kernel/installkernel-36",
            "[ebuild  N     ] sys-boot/grub-2.12-r5  USE=\"-fonts -themes\"",
            "",
            "Would you like to merge these packages? [Yes/No] y",
            ">>> Emerging (1 of 2) sys-kernel/installkernel-36::gentoo",
            ">>> Emerging (2 of 2) sys-boot/grub-2.12-r5::gentoo",
            ">>> Jobs: 2 of 2 complete");

        // ---------- kernel ----------
        s("cd\\s+/usr/src/linux", "cd /usr/src/linux", 1, true,
            " * now in /usr/src/linux -> linux-6.6.21-gentoo");

        s("make(\\s+-j\\s*\\d+)?", "make -j8", 3, true,
            "make[1]: Entering directory '/usr/src/linux-6.6.21-gentoo'",
            "  CC      init/main.o",
            "  CC      kernel/sched/core.o",
            "  CC      kernel/fork.o",
            "  CC      mm/memory.o",
            "  CC      fs/inode.o",
            "  CC      fs/ext4/super.o",
            "  CC      drivers/ata/libata-core.o",
            "  CC      drivers/net/ethernet/intel/e1000/e1000_main.o",
            "  CC      net/core/sock.o",
            "  LD      init/built-in.a",
            "  LD      vmlinux.o",
            "  MODPOST vmlinux.symvers",
            "  LD      vmlinux",
            "  SORTTAB vmlinux",
            "  OBJCOPY arch/x86/boot/compressed/vmlinux.bin",
            "  ZSTD    arch/x86/boot/compressed/vmlinux.bin.zst",
            "  HOSTCC  arch/x86/boot/tools/build",
            "  BUILD   arch/x86/boot/bzImage",
            "Kernel: arch/x86/boot/bzImage is ready  (#1)",
            "make[1]: Leaving directory '/usr/src/linux-6.6.21-gentoo'");

        s("make\\s+modules_install", "make modules_install", 3, true,
            "  INSTALL /lib/modules/6.6.21-gentoo/kernel/fs/ext4/ext4.ko",
            "  INSTALL /lib/modules/6.6.21-gentoo/kernel/drivers/ata/libata.ko",
            "  INSTALL /lib/modules/6.6.21-gentoo/kernel/drivers/net/e1000.ko",
            "  DEPMOD  6.6.21-gentoo");

        // ---------- bootloader ----------
        s("grub-install\\s+--target=x86_64-efi\\s+--efi-directory=/boot(\\s+--bootloader-id=\\w+)?", "grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=Gentoo", 2, true,
            "Installing for x86_64-efi platform.",
            "Installation finished. No error reported.");

        s("grub-mkconfig\\s+-o\\s+/boot/grub/grub\\.cfg", "grub-mkconfig -o /boot/grub/grub.cfg", 2, true,
            "Generating grub configuration file ...",
            "Found linux image: /boot/vmlinuz-6.6.21-gentoo",
            "Found initrd image: /boot/initramfs-6.6.21-gentoo.img",
            "Warning: os-prober will not be executed to detect other bootable partitions.",
            "done");

        // ---------- users ----------
        s("passwd(\\s+root)?", "passwd", 2, true,
            "New password:",
            "Retype new password:",
            "passwd: password updated successfully");

        s("useradd\\s+-m\\s+-G\\s+wheel\\s+-s\\s+/bin/bash\\s+mint", "useradd -m -G wheel -s /bin/bash mint", 2, true,
            " * user 'mint' created, home /home/mint, groups: mint wheel",
            " * default shell: /bin/bash");

        s("exit", "exit", 1, true,
            "logout",
            " * left the chroot, back on the live cd");

        // ---------- the moment ----------
        s("reboot", "reboot", 2, false,
            " * Unmounting filesystems ... [ ok ]",
            " * Remounting root filesystem read-only ... [ ok ]",
            " * Saving random seed ... [ ok ]",
            " * Syncing ... [ ok ]",
            "",
            " * Rebooting into your new Gentoo system",
            " * This may take a few seconds ...");
    }

    private Flow() {
    }
}
