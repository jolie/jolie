/*
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 *   For details about the authors of this software, see the AUTHORS file.
 */
package joliex.queryengine.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jolie.Jolie;

/**
 * This class implements {@link Path}s in {@link Jolie}. We use {@link Path}s in
 * the TQuery framework for ephemeral data handling over {@link Jolie} trees.
 *
 * @author Stefano Pio Zingaro <stefano.zingaro@icloud.com>
 */
public class Path {

	private static final String REGEX_STRNG = "\\.";

	private final String node;
	private final Path continuation;

	private Path(String node, Path continuation) {
		this.node = node;
		this.continuation = continuation;
	}

	/**
	 * It parses a
	 * <pre>String</pre>, e.g. <emph>a.b.c</emph>, and recursively build a
	 * {@link Path} with a node in the root and a {@link Path} as its
	 * continuation
	 *
	 * @param path the <pre>String</pre> representing the {@link Path}
	 *
	 * @return
	 */
	public static Path parse(String path) {
		return unfold(new ArrayList<>(Arrays.asList(path.split(REGEX_STRNG))));
	}

	private static Path unfold(List<String> nodes) {
		return nodes.isEmpty() ? null : new Path(nodes.remove(0), unfold(nodes));
	}

	/**
	 * It gives a nice
	 * <pre>String</pre> representation of a {@link Path}
	 *
	 * @return the <pre>String</pre> representation of this {@link Path}
	 */
	public String toPrettyString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.node);
		if (this.continuation != null) {
			sb.append(" - ");
			sb.append(this.continuation.toPrettyString());
		}
		return sb.toString();
	}
}
