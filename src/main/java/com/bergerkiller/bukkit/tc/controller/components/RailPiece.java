package com.bergerkiller.bukkit.tc.controller.components;

import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.offline.OfflineBlock;
import com.bergerkiller.bukkit.common.offline.OfflineWorld;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.rails.RailLookup;
import com.bergerkiller.bukkit.tc.rails.RailLookup.TrackedSign;
import com.bergerkiller.bukkit.tc.rails.type.RailType;
import com.bergerkiller.bukkit.tc.utils.RailJunctionSwitcher;

/**
 * Represents a single piece of rails, occupying a single block where signs can be placed.
 * A rail piece consists of a rail block and rail type bound to a single Block in the minecraft world.
 * Extra properties about the rail are cached for faster lookup of this rarely changing information.
 */
public class RailPiece {
    public static final RailPiece NONE = createWorldPlaceholder(null);
    private final RailType type;
    private final OfflineWorld world;
    private final Block block;
    private final OfflineBlock offlineBlock;

    /**
     * The cached rail piece is initialized with information from {@link RailLookup}
     * to provide information about members and signs on this rail piece. By default
     * is initialized with a placeholder that informs to perform this lookup.
     */
    protected RailLookup.CachedRailPiece cached;

    // Used for RailLookup.CachedRailPiece REMOVED default constant
    protected RailPiece() {
        this.world = OfflineWorld.NONE;
        this.offlineBlock = null;
        this.block = null;
        this.type = RailType.NONE;
        this.cached = null; // Should never even be used
    }

    private RailPiece(World world) {
        this.world = OfflineWorld.of(world);
        this.offlineBlock = null;
        this.block = null;
        this.type = RailType.NONE;
        this.cached = RailLookup.CachedRailPiece.NONE;
    }

    protected RailPiece(OfflineBlock offlineBlock, Block block, RailType type) {
        this.world = offlineBlock.getWorld();
        this.offlineBlock = offlineBlock;
        this.block = block;
        this.type = type;
        this.cached = RailLookup.CachedRailPiece.NONE;
    }

    /**
     * Gets the type of rail
     *
     * @return rail type
     */
    public RailType type() {
        return this.type;
    }

    /**
     * Gets the block where the rail is placed.
     * Returns null if there is no rail block referenced.
     * This is the case when {@link #isNone()} returns true.
     * 
     * @return rail block
     */
    public Block block() {
        return this.block;
    }

    /**
     * Returns {@link #block()} as an {@link OfflineBlock}, which has a faster
     * performance as keys in HashMaps.
     *
     * @return rail offline block
     */
    public OfflineBlock offlineBlock() {
        return this.offlineBlock;
    }

    /**
     * Gets an IntVector3 position of {@link #block()}
     *
     * @return block position
     */
    public IntVector3 blockPosition() {
        try {
            return this.offlineBlock.getPosition();
        } catch (NullPointerException ex) {
            if (this.offlineBlock == null) {
                throw new IllegalStateException("This rail piece is a world placeholder and has no rail block");
            }
            throw ex;
        }
    }

    /**
     * Gets whether this RailPiece and another one refer to the same rail block
     *
     * @param piece
     * @return True if this {@link #block()} and the piece's block are the same
     */
    public boolean isSameBlock(RailPiece piece) {
        return this.offlineBlock.equals(piece.offlineBlock);
    }

    /**
     * Gets whether this rail piece refers to missing rails.
     * The {@link #NONE} constant qualifies.
     * 
     * @return is none (block is null)
     */
    public boolean isNone() {
        return this.block == null;
    }

    /**
     * Asks the rail type whether the rail block of this rail piece uses block activation.
     * See also: {@link RailType#hasBlockActivation(Block)}.
     * 
     * @return True if block activation is used when driving on this rail piece
     */
    public boolean hasBlockActivation() {
        return this.type.hasBlockActivation(this.block);
    }

    /**
     * Gets the junctions that can possibly be switched on this rail piece
     * 
     * @return junctions
     */
    public List<RailJunction> getJunctions() {
        return this.type.getJunctions(this.block);
    }

    /**
     * Switches the rails from one junction to another. Junctions are used from
     * {@link #getJunctions()}. In addition to switching track, also moves carts
     * currently on this rail block along to the new junction, if possible.
     * 
     * @param railBlock where this Rail Type is at
     * @param from junction
     * @param to junction
     */
    public void switchJunction(RailJunction from, RailJunction to) {
        (new RailJunctionSwitcher(this)).switchJunction(from, to);
    }

    /**
     * Gets the world where the rail exists.
     * This will be available even when the rail block is not.
     * 
     * @return rail world
     */
    public World world() {
        return this.world.getLoadedWorld();
    }

    /**
     * Gets {@link #world()} as an {@link OfflineWorld}
     *
     * @return rail offline world
     */
    public OfflineWorld offlineWorld() {
        return this.world;
    }

    private RailLookup.CachedRailPiece accessCache() {
        RailLookup.CachedRailPiece cached = this.cached;
        if (cached.verify()) {
            return cached;
        } else {
            return this.cached = RailLookup.lookupCachedRailPiece(this.offlineBlock, this.block, this.type);
        }
    }

    /**
     * Retrieves an array of all signs active for this rail
     * 
     * @return array of signs
     */
    public TrackedSign[] signs() {
        return accessCache().cachedSigns();
    }

    /**
     * Gets a list of Minecart Members that occupy this rail piece.
     * The returned List is immutable.
     *
     * @return immutable members list
     */
    public List<MinecartMember<?>> members() {
        return accessCache().cachedMembers();
    }

    /**
     * Gets a list of Minecart Members that occupy this rail piece.
     * The returned List is guaranteed to be mutable.
     *
     * @return mutable members list
     */
    public List<MinecartMember<?>> mutableMembers() {
        return accessCache().cachedMutableMembers();
    }

    /**
     * Returns a new RailPiece with this same piece's world and block, but with
     * RailType NONE.
     *
     * @return new RailPiece with rail type NONE
     */
    public RailPiece asNoneType() {
        return new RailPiece(this.offlineBlock, this.block, RailType.NONE);
    }

    @Override
    public int hashCode() {
        return this.offlineBlock == null ? 0 : this.offlineBlock.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof RailPiece) {
            RailPiece other = (RailPiece) o;
            if (this.offlineBlock != null) {
                return this.offlineBlock.equals(other.offlineBlock) &&
                       this.type == other.type;
            } else {
                return other.offlineBlock == null &&
                       this.type == other.type;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.block == null) {
            return "{" + this.type + " ?/?/?}";
        } else {
            return "{" +
                      this.type + " " +
                      this.block.getX() + "/" +
                      this.block.getY() + "/" +
                      this.block.getZ() +
                   "}";
        }
    }

    /**
     * Creates a new RailBlock for a type and block
     * 
     * @param type
     * @param block
     * @return RailPiece
     */
    public static RailPiece create(RailType type, Block block) {
        return new RailPiece(OfflineBlock.of(block), block, type);
    }

    /**
     * Creates a new RailBlock for a type and block. World is also specified
     * to avoid having to look it up by Bukkit World.
     *
     * @param type Rail type
     * @param block Rail block
     * @param world Offline World
     * @return RailPiece
     */
    public static RailPiece create(RailType type, Block block, OfflineWorld world) {
        return new RailPiece(world.getBlockAt(block.getX(), block.getY(), block.getZ()), block, type);
    }

    /**
     * Creates a new RailBlock for a block. The rail type is inferred
     * from the rail block.
     * 
     * @param block
     * @return RailPiece
     */
    public static RailPiece create(Block block) {
        return new RailPiece(OfflineBlock.of(block), block, RailType.getType(block));
    }

    /**
     * Creates a RailBlock that has no rail type or block, and acts as a placeholder
     * to define for a RailState what world is used.
     * 
     * @param world
     * @return rail block defining the world
     */
    public static RailPiece createWorldPlaceholder(World world) {
        return new RailPiece(world);
    }
}
