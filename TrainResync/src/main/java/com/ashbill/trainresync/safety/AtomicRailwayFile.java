package com.ashbill.trainresync.safety;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;

/** The temporary file is also the persistent evidence of an uncommitted save. */
public final class AtomicRailwayFile {
    private AtomicRailwayFile() {}

    @FunctionalInterface
    public interface Writer {
        void write(OutputStream output) throws IOException;
    }

    public static Path temporary(Path target) {
        return target.resolveSibling(target.getFileName() + ".trainresync.tmp");
    }

    public static void checkState(Path target, boolean allowMissing) throws IOException {
        Path temporary = temporary(target);
        if (Files.exists(temporary, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Uncommitted railway save exists: " + temporary
                    + ". Inspect both files before manually recovering; automatic recovery is disabled.");
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Railway data must be a regular file: " + target);
            }
        } else if (!allowMissing) {
            throw new IOException("Existing world is missing railway data: " + target);
        }
    }

    public static void write(Path target, Writer writer) throws IOException {
        write(target, writer, (temporary, destination) ->
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE));
    }

    @FunctionalInterface
    interface Committer { void commit(Path temporary, Path target) throws IOException; }

    static void write(Path target, Writer writer, Committer committer) throws IOException {
        checkState(target, true);
        Path temporary = temporary(target);
        // CREATE_NEW both detects an old attempt and creates evidence before NBT serialization.
        Files.createFile(temporary);
        // Never delete the temporary file on failure, and never fall back to a non-atomic move.
        try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.WRITE)) {
            writer.write(output);
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            preserveAccess(target, temporary);
        }
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
        committer.commit(temporary, target);
    }

    private static void preserveAccess(Path target, Path temporary) throws IOException {
        PosixFileAttributeView source = Files.getFileAttributeView(target,
                PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (source != null) {
            var attributes = source.readAttributes();
            var destination = Files.getFileAttributeView(temporary, PosixFileAttributeView.class);
            var current = destination.readAttributes();
            if (!attributes.owner().equals(current.owner())) destination.setOwner(attributes.owner());
            if (!attributes.group().equals(current.group())) destination.setGroup(attributes.group());
            destination.setPermissions(attributes.permissions());
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(target,
                AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl != null) {
            var destination = Files.getFileAttributeView(temporary, AclFileAttributeView.class);
            if (!acl.getOwner().equals(destination.getOwner())) destination.setOwner(acl.getOwner());
            destination.setAcl(acl.getAcl());
        }
    }
}
