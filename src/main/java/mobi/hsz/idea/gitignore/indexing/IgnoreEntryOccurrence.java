/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mobi.hsz.idea.gitignore.indexing;

import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry containing information about the {@link VirtualFile} instance of the ignore file mapped with the collection
 * of ignore entries for better performance. Class is used for indexing.
 *
 * @author Jakub Chrzanowski <jakub@hsz.mobi>
 * @since 2.0
 */
public class IgnoreEntryOccurrence implements Serializable {
    /** Current ignore file path. */
    @Nonnull
    private final String url;

    /** Collection of ignore entries. */
    @Nonnull
    private final List<Pair<String, Boolean>> items;

    /** Current ignore file. */
    @Nullable
    private VirtualFile file;

    /**
     * Constructor.
     *
     * @param url   entry URL
     * @param items parsed entry items
     */
    public IgnoreEntryOccurrence(@Nonnull String url, @Nonnull ArrayList<Pair<String, Boolean>> items) {
        this.url = url;
        this.items = List.copyOf(items);
    }

    /**
     * Calculates hashCode with {@link #url} and {@link #items} hashCodes.
     *
     * @return entry hashCode
     */
    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder().append(url);

        for (Pair<String, Boolean> item : items) {
            builder.append(item.first).append(item.second);
        }

        return builder.toHashCode();
    }

    /**
     * Checks if given object is equal to current {@link IgnoreEntryOccurrence} instance.
     *
     * @param obj to check
     * @return objects are equal.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof IgnoreEntryOccurrence entry) {
            if (!url.equals(entry.url) || items.size() != entry.items.size()) {
                return false;
            }

            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).toString().equals(entry.items.get(i).toString())) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Returns current {@link VirtualFile}.
     *
     * @return current file
     */
    @Nullable
    public VirtualFile getFile() {
        if (file == null && !url.isEmpty()) {
            file = VirtualFileManager.getInstance().findFileByUrl(url);
        }
        return file;
    }

    /**
     * Returns entries for current file.
     *
     * @return entries
     */
    @Nonnull
    public List<Pair<String, Boolean>> getItems() {
        return items;
    }

    /**
     * Static helper to write given {@link IgnoreEntryOccurrence} to the output stream.
     *
     * @param out   output stream
     * @param entry entry to write
     * @throws IOException I/O exception
     */
    public static synchronized void serialize(@Nonnull DataOutput out, @Nonnull IgnoreEntryOccurrence entry) throws IOException {
        out.writeUTF(entry.url);
        out.writeInt(entry.items.size());
        for (Pair<String, Boolean> item : entry.items) {
            out.writeUTF(item.first);
            out.writeBoolean(item.second);
        }
    }

    /**
     * Static helper to read {@link IgnoreEntryOccurrence} from the input stream.
     *
     * @param in input stream
     * @return read {@link IgnoreEntryOccurrence}
     */
    @Nonnull
    public static synchronized IgnoreEntryOccurrence deserialize(@Nonnull DataInput in) throws IOException {
        String url = in.readUTF();
        ArrayList<Pair<String, Boolean>> items = new ArrayList<>();

        if (!StringUtil.isEmpty(url)) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String pattern = in.readUTF();
                Boolean isNegated = in.readBoolean();
                items.add(Pair.create(pattern, isNegated));
            }
        }

        return new IgnoreEntryOccurrence(url, items);
    }
}
