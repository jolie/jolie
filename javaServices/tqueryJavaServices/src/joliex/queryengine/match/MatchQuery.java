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
package joliex.queryengine.match;

import java.util.HashMap;
import java.util.Optional;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import static joliex.queryengine.common.Utils.Constants.*;

public final class MatchQuery {

	private static BinaryExpression createBinaryExpression(ValueVector children) {
		HashMap<String, ValueVector> leftRight = new HashMap<>();

		children.forEach(
				value -> {
					if (value.hasChildren(left)) {
						leftRight.put(left, value.getChildren(left));
					}

					if (value.hasChildren(right)) {
						leftRight.put(right, value.getChildren(right));
					}
				});
		assert (leftRight.containsKey(left) && leftRight.containsKey(right));

		return new BinaryExpression(createMatchExpression(leftRight.get(left).first()),
				createMatchExpression(leftRight.get(right).first()));
	}

	private static CompareExp createCompareExpression(ValueVector children) {
		HashMap<String, ValueVector> pathValue = new HashMap<>();

		children.forEach(
				value -> {
					if (value.hasChildren(path)) {
						pathValue.put(path, value.getChildren(path));
					}

					if (value.hasChildren(val)) {
						pathValue.put(val, value.getChildren(val));
					}
				});
		assert (pathValue.containsKey(path) && pathValue.containsKey(val));
		assert ((pathValue.get(path).size() == 1) && (pathValue.get(val).size() == 1));

		return new CompareExp(pathValue.get(path).first().strValue(), pathValue.get(val).first().strValue());
	}

	public static MatchExpression createMatchExpression(Value query) {
		try {
			if (query.hasChildren(and)) {
				ValueVector children = query.getChildren(and);

				return new AndExp(createBinaryExpression(children));
			}

			if (query.hasChildren(or)) {
				ValueVector children = query.getChildren(or);

				return new OrExp(createBinaryExpression(children));
			}

			if (query.hasChildren(not)) {
				ValueVector children = query.getChildren(not);

				assert (children.size() == 1);

				return new NotExp(createMatchExpression(children.first()));
			}

			if (query.hasChildren(equal)) {
				ValueVector children = query.getChildren(equal);

				return new EqualExp(createCompareExpression(children));
			}

			if (query.hasChildren(greaterThen)) {
				ValueVector children = query.getChildren(greaterThen);

				assert (children.size() == 1);

				return new GreaterThenExp(createCompareExpression(children));
			}

			if (query.hasChildren(lowerThen)) {
				ValueVector children = query.getChildren(lowerThen);

				assert (children.size() == 1);

				return new LowerThenExp(createCompareExpression(children));
			}

			if (query.hasChildren(exists)) {
				ValueVector children = query.getChildren(exists);

				assert (children.size() == 1);

				return new ExistsExp(children.first().strValue());
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		throw new IllegalArgumentException("Unsupported structure of a match request");
	}

	public static Optional<Value> match(Value matchRequest) {
		Value query = matchRequest.getChildren("query").first();
		Value data = matchRequest.getChildren("data").first();
		MatchExpression matchExpression = createMatchExpression(query);

		return matchExpression.interpret(data);
	}
}


//~ Formatted by Jindent --- http://www.jindent.com
