package com.licht_meilleur.blue_student.network;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.craft_chamber.CraftChamberRecipes;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.AliceEntity;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.entity.HoshinoEntity;
import com.licht_meilleur.blue_student.entity.KisakiEntity;
import com.licht_meilleur.blue_student.entity.MarieEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.ShirokoEntity;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentId;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class ModPackets {

    private ModPackets() {
    }

    private static final int COST_DIAMOND = 64;

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC =
            new StreamCodec<>() {
                @Override
                public BlockPos decode(RegistryFriendlyByteBuf buf) {
                    return buf.readBlockPos();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, BlockPos value) {
                    buf.writeBlockPos(value);
                }
            };

    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, Vec3::x,
                    ByteBufCodecs.DOUBLE, Vec3::y,
                    ByteBufCodecs.DOUBLE, Vec3::z,
                    Vec3::new
            );

    public record SetAiModePayload(int entityId, int modeId) implements CustomPacketPayload {
        public static final Identifier ID = BlueStudentMod.id("set_ai_mode");
        public static final Type<SetAiModePayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, SetAiModePayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, SetAiModePayload::entityId,
                        ByteBufCodecs.INT, SetAiModePayload::modeId,
                        SetAiModePayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CallStudentPayload(String studentId, BlockPos tabletPos) implements CustomPacketPayload {
        public static final Identifier ID = BlueStudentMod.id("call_student");
        public static final Type<CallStudentPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, CallStudentPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CallStudentPayload::studentId,
                        BLOCK_POS_CODEC, CallStudentPayload::tabletPos,
                        CallStudentPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CallBackStudentPayload(String studentId, BlockPos tabletPos) implements CustomPacketPayload {
        public static final Identifier ID = BlueStudentMod.id("call_back_student");
        public static final Type<CallBackStudentPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, CallBackStudentPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CallBackStudentPayload::studentId,
                        BLOCK_POS_CODEC, CallBackStudentPayload::tabletPos,
                        CallBackStudentPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CraftChamberCraftPayload(BlockPos chamberPos, int pageIndex) implements CustomPacketPayload {
        public static final Identifier ID = BlueStudentMod.id("craft_chamber_craft");
        public static final Type<CraftChamberCraftPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, CraftChamberCraftPayload> CODEC =
                StreamCodec.composite(
                        BLOCK_POS_CODEC, CraftChamberCraftPayload::chamberPos,
                        ByteBufCodecs.INT, CraftChamberCraftPayload::pageIndex,
                        CraftChamberCraftPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ShotFxPayload(
            int shooterEntityId,
            Vec3 start,
            int fxTypeOrdinal,
            float fxWidth,
            float travelDist,
            List<Vec3> dirs
    ) implements CustomPacketPayload {
        public static final Identifier ID = BlueStudentMod.id("s2c_shot_fx");
        public static final Type<ShotFxPayload> TYPE = new Type<>(ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, ShotFxPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, ShotFxPayload::shooterEntityId,
                        VEC3_CODEC, ShotFxPayload::start,
                        ByteBufCodecs.INT, ShotFxPayload::fxTypeOrdinal,
                        ByteBufCodecs.FLOAT, ShotFxPayload::fxWidth,
                        ByteBufCodecs.FLOAT, ShotFxPayload::travelDist,
                        VEC3_CODEC.apply(ByteBufCodecs.list()), ShotFxPayload::dirs,
                        ShotFxPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(SetAiModePayload.TYPE, SetAiModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CallStudentPayload.TYPE, CallStudentPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CallBackStudentPayload.TYPE, CallBackStudentPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CraftChamberCraftPayload.TYPE, CraftChamberCraftPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(ShotFxPayload.TYPE, ShotFxPayload.CODEC);
    }

    public static void registerC2S() {
        ServerPlayNetworking.registerGlobalReceiver(SetAiModePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();

            context.server().execute(() -> {
                var raw = player.level().getEntity(payload.entityId());
                if (!(raw instanceof AbstractStudentEntity student)) {
                    return;
                }

                UUID owner = student.getOwnerUuid();
                if (owner == null || !owner.equals(player.getUUID())) {
                    return;
                }

                StudentAiMode mode = StudentAiMode.fromId(payload.modeId());
                if (mode == null) {
                    return;
                }

                student.setAiMode(mode);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CallStudentPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();

            server.execute(() -> {
                try {
                    ServerLevel level = (ServerLevel) player.level();
                    StudentWorldState state = StudentWorldState.get(level);

                    StudentId sid = parseStudentId(payload.studentId());
                    BlockPos tabletPos = payload.tabletPos();

                    if (state.hasStudent(sid)) {
                        player.sendSystemMessage(Component.literal("Already summoned"));
                        return;
                    }

                    if (!player.getAbilities().instabuild) {
                        int ticketCount = countItem(player, BlueStudentMod.TICKET);
                        int diamondCount = countItem(player, Items.DIAMOND);

                        if (ticketCount < 1 && diamondCount < COST_DIAMOND) {
                            player.sendSystemMessage(Component.literal(
                                    "Not enough cost. Need 1 Ticket or " + COST_DIAMOND + " Diamonds."
                            ));
                            return;
                        }
                    }

                    Entity raw = switch (sid) {
                        case SHIROKO -> new ShirokoEntity(BlueStudentMod.SHIROKO, level);
                        case HOSHINO -> new HoshinoEntity(BlueStudentMod.HOSHINO, level);
                        case HINA -> new HinaEntity(BlueStudentMod.HINA, level);
                        case ALICE -> new AliceEntity(BlueStudentMod.ALICE, level);
                        case KISAKI -> new KisakiEntity(BlueStudentMod.KISAKI, level);
                        case MARIE -> new MarieEntity(BlueStudentMod.MARIE, level);
                        case HIKARI -> new HikariEntity(BlueStudentMod.HIKARI, level);
                        case NOZOMI -> new NozomiEntity(BlueStudentMod.NOZOMI, level);
                    };

                    if (!(raw instanceof IStudentEntity se)) {
                        player.sendSystemMessage(Component.literal("Spawn failed (type mismatch)"));
                        return;
                    }

                    BlockPos spawn = tabletPos.above();

                    raw.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                    raw.setYRot(player.getYRot());
                    raw.setXRot(0.0f);

                    UUID owner = player.getUUID();
                    se.setOwnerUuid(owner);

                    boolean ok = level.addFreshEntity(raw);
                    if (!ok) {
                        player.sendSystemMessage(Component.literal("Spawn failed"));
                        return;
                    }

                    if (!consumeSummonCost(player)) {
                        player.sendSystemMessage(Component.literal(
                                "Not enough cost. Need 1 Ticket (preferred) or 64 Diamonds."
                        ));
                        return;
                    }

                    state.setStudent(sid, raw.getUUID(), owner, level, spawn);
                    player.sendSystemMessage(Component.literal("Summoned"));

                } catch (Throwable t) {
                    BlueStudentMod.LOGGER.error("[BlueStudent] CALL crashed", t);
                    player.sendSystemMessage(Component.literal("CALL crashed. See log."));
                }
            });

        });

        ServerPlayNetworking.registerGlobalReceiver(CallBackStudentPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();

            server.execute(() -> {
                ServerLevel level = (ServerLevel) player.level();
                StudentWorldState state = StudentWorldState.get(level);

                StudentId sid = parseStudentId(payload.studentId());
                UUID uuid = state.getStudentUuid(sid);
                if (uuid == null) return;

                BlockPos spawn = payload.tabletPos().above();

                // まず今のディメンションにいるか確認
                Entity found = level.getEntity(uuid);
                if (found != null && found.isAlive()) {
                    if (!(found instanceof AbstractStudentEntity student)) {
                        return;
                    }

                    UUID owner = student.getOwnerUuid();
                    if (owner == null || !owner.equals(player.getUUID())) {
                        return;
                    }

                    found.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                    found.setYRot(player.getYRot());
                    found.setXRot(0.0f);
                    state.setStudent(sid, found.getUUID(), owner, level, spawn);
                    return;
                }

                // 別ディメンション側を探す
                StudentWorldState.StudentData data = state.getData(sid);
                if (data == null || data.dimension == null) return;

                ResourceKey<Level> key = ResourceKey.create(
                        Registries.DIMENSION,
                        Identifier.parse(data.dimension)
                );

                ServerLevel oldLevel = server.getLevel(key);
                if (oldLevel == null) return;

                Entity other = oldLevel.getEntity(uuid);
                if (!(other instanceof AbstractStudentEntity ase) || !other.isAlive()) return;

                UUID owner = ase.getOwnerUuid();
                if (owner == null || !owner.equals(player.getUUID())) {
                    return;
                }

                if (ase.isLifeLockedForGoal()) {
                    player.sendSystemMessage(Component.literal("Student is respawning..."));
                    return;
                }

                boolean ok = ase.teleportToWorldForCallback(level, spawn, player.getYRot());
                if (ok) {
                    state.setStudent(sid, ase.getUUID(), owner, level, spawn);
                }
            });

        });

        ServerPlayNetworking.registerGlobalReceiver(CraftChamberCraftPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();

            server.execute(() -> {
                try {
                    ServerLevel level = (ServerLevel) player.level();
                    BlockPos chamberPos = payload.chamberPos();

                    if (player.distanceToSqr(
                            chamberPos.getX() + 0.5,
                            chamberPos.getY() + 0.5,
                            chamberPos.getZ() + 0.5
                    ) > 64) {
                        return;
                    }

                    var be = level.getBlockEntity(chamberPos);
                    if (!(be instanceof CraftChamberBlockEntity)) {
                        return;
                    }

                    var recipe = CraftChamberRecipes.byIndex(payload.pageIndex());
                    boolean creative = player.getAbilities().instabuild;

                    if (!creative) {
                        for (var cost : recipe.costs()) {
                            if (countItem(player, cost.item()) < cost.count()) {
                                player.sendSystemMessage(Component.literal("Not enough materials"));
                                return;
                            }
                        }

                        for (var cost : recipe.costs()) {
                            removeItem(player, cost.item(), cost.count());
                        }
                    }

                    ItemStack out = recipe.output().copy();

                    ItemEntity drop = new ItemEntity(
                            level,
                            chamberPos.getX() + 0.5,
                            chamberPos.getY() + 1.1,
                            chamberPos.getZ() + 0.5,
                            out
                    );
                    drop.setDefaultPickUpDelay();
                    drop.setDeltaMovement(0, 0.20, 0);
                    level.addFreshEntity(drop);

                    level.playSound(
                            null,
                            chamberPos,
                            net.minecraft.sounds.SoundEvents.ANVIL_USE,
                            net.minecraft.sounds.SoundSource.BLOCKS,
                            0.6f,
                            1.2f
                    );

                } catch (Throwable t) {
                    BlueStudentMod.LOGGER.error("[BlueStudent] CRAFT_CHAMBER_CRAFT crashed", t);
                    player.sendSystemMessage(Component.literal("Craft failed. See log."));
                }
            });
        });
    }

    private static StudentId parseStudentId(String s) {
        for (StudentId id : StudentId.values()) {
            if (id.asString().equalsIgnoreCase(s)) return id;
            if (id.name().equalsIgnoreCase(s)) return id;
        }
        throw new IllegalArgumentException("Unknown StudentId: " + s);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static void removeItem(ServerPlayer player, Item item, int amount) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(item)) continue;

            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
    }

    private static boolean consumeSummonCost(ServerPlayer player) {
        if (player.isCreative()) return true;

        if (consumeCount(player, BlueStudentMod.TICKET, 1)) {
            return true;
        }

        return consumeCount(player, Items.DIAMOND, COST_DIAMOND);
    }

    private static boolean consumeCount(ServerPlayer player, Item item, int amount) {
        if (amount <= 0) return true;

        var inv = player.getInventory();

        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.is(item)) total += st.getCount();
            if (total >= amount) break;
        }
        if (total < amount) return false;

        int remain = amount;
        for (int i = 0; i < inv.getContainerSize() && remain > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty() || !st.is(item)) continue;

            int take = Math.min(remain, st.getCount());
            st.shrink(take);
            remain -= take;
        }

        player.containerMenu.broadcastChanges();
        return true;
    }

    public record CraftChamberPayload(String pos, int page)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<CraftChamberPayload> TYPE =
                new CustomPacketPayload.Type<>(BlueStudentMod.id("craft_chamber"));

        public static final StreamCodec<FriendlyByteBuf, CraftChamberPayload> CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeUtf(p.pos());
                            buf.writeInt(p.page());
                        },
                        buf -> new CraftChamberPayload(
                                buf.readUtf(),
                                buf.readInt()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}