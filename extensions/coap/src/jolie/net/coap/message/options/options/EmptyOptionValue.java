package jolie.net.coap.message.options.options;

import java.util.Arrays;

public final class EmptyOptionValue extends OptionValue<Void> {

    public EmptyOptionValue(int optionNumber) throws IllegalArgumentException {
	super(optionNumber, new byte[0], false);
    }

    @Override
    public Void getDecodedValue() {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EmptyOptionValue))
            return false;

        EmptyOptionValue other = (EmptyOptionValue) object;
        return Arrays.equals(this.getValue(), other.getValue());
    }
}
