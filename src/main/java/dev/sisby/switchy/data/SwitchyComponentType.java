package dev.sisby.switchy.data;

import dev.sisby.switchy.exception.ComponentFailedInitializeException;
import dev.sisby.switchy.exception.NbtException;
import dev.sisby.switchy.util.TypeRegistry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface SwitchyComponentType<T> extends TypeRegistry.Type {
    static <T> SwitchyComponentType.Builder<T> builder(ResourceLocation id) {
        return new SwitchyComponentType.Builder<>(id);
    }

    @Nullable Initializer<T> initializer();

    @Nullable NbtReader<T> nbtReader();

    @Nullable NbtMutator<T> nbtMutator();

    @Nullable PlayerReader<T> playerReader();

    @Nullable PlayerMutator<T> playerMutator();

    @Nullable EmptyChecker<T> emptyChecker();

    @Nullable TextProvider<T> textProvider();

    @Nullable NBTSerializer<T> nbtSerializer();

    @Nullable ResourceLocation group();

    boolean hidden();

    int previewPriority();

    default void tryInitialize(Collection<SwitchyComponentMap> consumer, NBTTagCompound nbt, EntityPlayerMP player, String profileId) {
        Initializer<T> initializer = initializer();
        if (initializer == null) return;
        T value = initializer.initialize(nbt, player, profileId);
        for (SwitchyComponentMap c : consumer) {
            c.set(this, value);
        }
    }

    default void tryMutate(SwitchyComponentMap components, NBTTagCompound playerData, EntityPlayerMP player) throws NbtException {
        NbtMutator<T> nbtMutator = nbtMutator();
        PlayerMutator<T> playerMutator = playerMutator();
        if (nbtMutator != null) {
            nbtMutator.mutate(components.get(this), playerData);
        } else if (playerMutator != null) {
            playerMutator.mutate(components.get(this), player);
        }
    }

    default ITextComponent asText(T value) {
        TextProvider<T> textProvider = textProvider();
        if (textProvider != null) {
            return textProvider.toText(value);
        } else {
            return new TextComponentString(Objects.toString(value));
        }
    }

    default ITextComponent asText(SwitchyComponentMap components) {
        return asText(components.get(this));
    }

    default boolean isPrecious(SwitchyComponentMap components) {
        EmptyChecker<T> emptyChecker = emptyChecker();
        if (emptyChecker != null) {
            return !emptyChecker.isEmpty(components.get(this));
        }
        return false;
    }

    default NBTBase toNBT(Object value) {
        NBTSerializer<T> serializer = nbtSerializer();
        if (serializer != null) {
            return serializer.toNBT((T) value);
        }
        return new NBTTagCompound();
    }

    default Object fromNBT(NBTBase nbt) {
        NBTSerializer<T> serializer = nbtSerializer();
        if (serializer != null) {
            return serializer.fromNBT(nbt);
        }
        return null;
    }

    @FunctionalInterface
    interface Initializer<T> {
        T initialize(NBTTagCompound playerNbt, EntityPlayerMP player, String profileId) throws ComponentFailedInitializeException;
    }

    @FunctionalInterface
    interface NbtReader<T> {
        T read(NBTTagCompound nbt) throws NbtException;
    }

    @FunctionalInterface
    interface NbtMutator<T> {
        void mutate(T value, NBTTagCompound nbt) throws NbtException;
    }

    @FunctionalInterface
    interface PlayerReader<T> {
        T read(EntityPlayerMP player, String profileId);
    }

    @FunctionalInterface
    interface PlayerMutator<T> {
        void mutate(T value, EntityPlayerMP player);
    }

    @FunctionalInterface
    interface EmptyChecker<T> {
        boolean isEmpty(T value);
    }

    @FunctionalInterface
    interface TextProvider<T> {
        ITextComponent toText(T value);
    }

    interface NBTSerializer<T> {
        NBTBase toNBT(T value);
        T fromNBT(NBTBase nbt);
    }

    class SimpleTextProvider<T> implements TextProvider<T> {
        private final Function<T, ITextComponent> provider;

        public SimpleTextProvider(Function<T, ITextComponent> provider) {
            this.provider = provider;
        }

        @Override
        public ITextComponent toText(T value) {
            return value == null ? new TextComponentString("") : provider.apply(value);
        }
    }

    class SimpleEmptyChecker<T> implements EmptyChecker<T> {
        private final Predicate<T> predicate;

        public SimpleEmptyChecker(Predicate<T> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean isEmpty(T value) {
            return predicate.test(value);
        }
    }

    class CopyInitializer<T> implements Initializer<T> {
        private final NbtReader<T> nbtReader;

        public CopyInitializer(NbtReader<T> nbtReader) {
            this.nbtReader = nbtReader;
        }

        @Override
        public T initialize(NBTTagCompound playerNbt, EntityPlayerMP player, String profileId) throws ComponentFailedInitializeException {
            try {
                return nbtReader.read(playerNbt);
            } catch (Exception e) {
                throw new ComponentFailedInitializeException("", e);
            }
        }
    }

    class NbtSwitcher<T> implements NbtMutator<T>, NbtReader<T> {
        private final String nbtPath;
        private final NBTSerializer<T> serializer;

        public NbtSwitcher(String nbtPath, NBTSerializer<T> serializer) {
            this.nbtPath = nbtPath;
            this.serializer = serializer;
        }

        @Override
        public T read(NBTTagCompound nbt) throws NbtException {
            if (!nbt.hasKey(nbtPath)) {
                return null;
            }
            NBTBase tag = nbt.getTag(nbtPath);
            return serializer.fromNBT(tag);
        }

        @Override
        public void mutate(T value, NBTTagCompound nbt) throws NbtException {
            if (value == null) {
                nbt.removeTag(nbtPath);
                return;
            }
            nbt.setTag(nbtPath, serializer.toNBT(value));
        }

        @Override
        public String toString() {
            return nbtPath;
        }
    }

    class SimpleSwitchyComponentType<T> implements SwitchyComponentType<T> {
        private final ResourceLocation id;
        @Nullable private final Initializer<T> initializer;
        @Nullable private final NbtReader<T> nbtReader;
        @Nullable private final NbtMutator<T> nbtMutator;
        @Nullable private final PlayerReader<T> playerReader;
        @Nullable private final PlayerMutator<T> playerMutator;
        @Nullable private final EmptyChecker<T> emptyChecker;
        @Nullable private final TextProvider<T> textProvider;
        @Nullable private final NBTSerializer<T> nbtSerializer;
        @Nullable private final ResourceLocation group;
        private final boolean hidden;
        private final int previewPriority;

        public SimpleSwitchyComponentType(
            ResourceLocation id,
            @Nullable Initializer<T> initializer,
            @Nullable NbtReader<T> nbtReader,
            @Nullable NbtMutator<T> nbtMutator,
            @Nullable PlayerReader<T> playerReader,
            @Nullable PlayerMutator<T> playerMutator,
            @Nullable EmptyChecker<T> emptyChecker,
            @Nullable TextProvider<T> textProvider,
            @Nullable NBTSerializer<T> nbtSerializer,
            @Nullable ResourceLocation group,
            boolean hidden,
            int previewPriority
        ) {
            this.id = id;
            this.initializer = initializer;
            this.nbtReader = nbtReader;
            this.nbtMutator = nbtMutator;
            this.playerReader = playerReader;
            this.playerMutator = playerMutator;
            this.emptyChecker = emptyChecker;
            this.textProvider = textProvider;
            this.nbtSerializer = nbtSerializer;
            this.group = group;
            this.hidden = hidden;
            this.previewPriority = previewPriority;
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @Override
        public Initializer<T> initializer() {
            return initializer;
        }

        @Override
        public NbtReader<T> nbtReader() {
            return nbtReader;
        }

        @Override
        public NbtMutator<T> nbtMutator() {
            return nbtMutator;
        }

        @Override
        public PlayerReader<T> playerReader() {
            return playerReader;
        }

        @Override
        public PlayerMutator<T> playerMutator() {
            return playerMutator;
        }

        @Override
        public EmptyChecker<T> emptyChecker() {
            return emptyChecker;
        }

        @Override
        public TextProvider<T> textProvider() {
            return textProvider;
        }

        @Override
        public NBTSerializer<T> nbtSerializer() {
            return nbtSerializer;
        }

        @Override
        public ResourceLocation group() {
            return group;
        }

        @Override
        public boolean hidden() {
            return hidden;
        }

        @Override
        public int previewPriority() {
            return previewPriority;
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }

    class Builder<T> {
        private final ResourceLocation id;
        @Nullable private Initializer<T> initializer;
        @Nullable private NbtReader<T> nbtReader;
        @Nullable private NbtMutator<T> nbtMutator;
        @Nullable private PlayerReader<T> playerReader;
        @Nullable private PlayerMutator<T> playerMutator;
        @Nullable private EmptyChecker<T> emptyChecker;
        @Nullable private TextProvider<T> textProvider;
        @Nullable private NBTSerializer<T> nbtSerializer;
        @Nullable private ResourceLocation group;
        private boolean hidden = false;
        private int previewPriority = 0;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder<T> initializer(@Nullable Initializer<T> initializer) {
            if (initializer != null) this.initializer = initializer;
            return this;
        }

        public Builder<T> playerReader(@Nullable PlayerReader<T> playerReader) {
            this.playerReader = playerReader;
            return this;
        }

        public Builder<T> playerMutator(@Nullable PlayerMutator<T> playerMutator) {
            this.playerMutator = playerMutator;
            return this;
        }

        public Builder<T> emptyChecker(@Nullable EmptyChecker<T> emptyChecker) {
            this.emptyChecker = emptyChecker;
            return this;
        }

        public Builder<T> textProvider(@Nullable TextProvider<T> textProvider) {
            this.textProvider = textProvider;
            return this;
        }

        public Builder<T> nbtSerializer(@Nullable NBTSerializer<T> nbtSerializer) {
            this.nbtSerializer = nbtSerializer;
            return this;
        }

        public Builder<T> nbtSwitcher(String path, NBTSerializer<T> serializer) {
            NbtSwitcher<T> switcher = new NbtSwitcher<>(path, serializer);
            this.nbtReader = switcher;
            this.nbtMutator = switcher;
            this.initializer = new CopyInitializer<>(switcher);
            this.nbtSerializer = serializer;
            return this;
        }

        public Builder<T> group(@Nullable ResourceLocation group) {
            this.group = group;
            return this;
        }

        public Builder<T> hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder<T> previewPriority(int priority) {
            this.previewPriority = priority;
            return this;
        }

        public SwitchyComponentType<T> build() {
            return new SwitchyComponentType.SimpleSwitchyComponentType<>(
                this.id,
                this.initializer,
                this.nbtReader,
                this.nbtMutator,
                this.playerReader,
                this.playerMutator,
                this.emptyChecker,
                this.textProvider,
                this.nbtSerializer,
                this.group,
                this.hidden,
                this.previewPriority
            );
        }
    }
}
