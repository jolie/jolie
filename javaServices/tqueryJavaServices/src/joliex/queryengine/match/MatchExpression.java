/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         * 
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.queryengine.match;

import java.util.Optional;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Utils;

final class AndExpression implements MatchExpression {

	private BinaryExpression and;

	AndExpression(BinaryExpression and) {
		this.and = and;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		return (and.getLeft().applyOn(data).isPresent() && and.getRight().applyOn(data).isPresent())
				? Optional.of(data)
				: Optional.empty();
	}
}

final class BinaryExpression {

	private MatchExpression left;     // .left: MatchExp
	private MatchExpression right;    // .right: MatchExp

	public BinaryExpression(MatchExpression left, MatchExpression right) {
		this.left = left;
		this.right = right;
	}

	MatchExpression getLeft() {
		return left;
	}

	MatchExpression getRight() {
		return right;
	}
}

final class CompareExp {

	private String path;     // .path: Path
	private String value;    // .value[1,*]: undefined

	CompareExp(String path, String value) {
		this.path = path;
		this.value = value;
	}

	String getPath() {
		return path;
	}

	String getValue() {
		return value;
	}
}

final class EqualExp extends UnaryExpression {

	private CompareExp equal;    // .equal: CompareExp

	EqualExp(CompareExp equal) {
		this.equal = equal;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		ValueVector values = Utils.evaluatePath(data, equal.getPath());

		return ((values.size() == 1) && values.first().strValue().equals(equal.getValue()))
				? Optional.of(data)
				: Optional.empty();
	}
}

final class ExistsExp extends UnaryExpression {

	private String exists;    // .exists: Path

	ExistsExp(String exists) {
		this.exists = exists;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		return (Utils.evaluatePath(data, exists).isEmpty())
				? Optional.empty()
				: Optional.of(data);
	}
}

final class GreaterThenExp extends UnaryExpression {

	private CompareExp greaterThen;    // .greaterThen: CompareExp

	GreaterThenExp(CompareExp greaterThen) {
		this.greaterThen = greaterThen;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		ValueVector values = Utils.evaluatePath(data, greaterThen.getPath());

		if (values.size() == 1) {
			return Utils.isGreater(values.first(), greaterThen.getValue())
					? Optional.of(data)
					: Optional.empty();
		} else {
			return Optional.empty();
		}
	}
}

final class LowerThenExp extends UnaryExpression {

	private CompareExp lowerThen;    // .lowerThen: CompareExp

	LowerThenExp(CompareExp lowerThen) {
		this.lowerThen = lowerThen;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		ValueVector values = Utils.evaluatePath(data, lowerThen.getPath());

		if (values.size() == 1) {
			return Utils.isGreater(values.first(), lowerThen.getValue())
					? Optional.empty()
					: Optional.of(data);
		} else {
			return Optional.empty();
		}
	}
}

final class NotExp implements MatchExpression {

	private MatchExpression not;    // .not: BinaryExp

	NotExp(MatchExpression not) {
		this.not = not;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		return (not.applyOn(data).isPresent())
				? Optional.empty()
				: Optional.of(data);
	}
}

final class OrExp implements MatchExpression {

	private BinaryExpression or;

	OrExp(BinaryExpression or) {
		this.or = or;
	}

	@Override
	public Optional<Value> applyOn(Value data) {
		return (or.getLeft().applyOn(data).isPresent() || or.getRight().applyOn(data).isPresent())
				? Optional.of(data)
				: Optional.empty();
	}
}

abstract class UnaryExpression implements MatchExpression {}

public interface MatchExpression {
	Optional<Value> applyOn( ValueVector data );
}
