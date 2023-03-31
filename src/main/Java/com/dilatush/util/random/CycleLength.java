package com.dilatush.util.random;

import java.math.BigInteger;

import static com.dilatush.util.General.isNull;

public record CycleLength( CycleLengthForm form, BigInteger cycleLength ) {


    public CycleLength {

        // sanity check...
        if( isNull( form ) ) throw new IllegalArgumentException( "form may not be null" );
        if( (form == CycleLengthForm.INFINITE ) && (!isNull( cycleLength )) ) throw new IllegalArgumentException( "INFINITE form may not specify a cycle length" );
        if( (form == CycleLengthForm.UNKNOWN  ) && (!isNull( cycleLength )) ) throw new IllegalArgumentException( "UNKNOWN form may not specify a cycle length"  );
        if( (form == CycleLengthForm.EXACTLY  ) && ( isNull( cycleLength )) ) throw new IllegalArgumentException( "EXACTLY form must specify a cycle length"     );
        if( (form == CycleLengthForm.AT_LEAST ) && ( isNull( cycleLength )) ) throw new IllegalArgumentException( "AT_LEAST form must specify a cycle length"    );
        if( cycleLength.compareTo( BigInteger.ZERO ) <= 0                   ) throw new IllegalArgumentException( "cycle length must be >+ 1"                    );
    }


    public CycleLength( CycleLengthForm form ) {
        this( form, null );
    }


    public boolean hasLength() {
        return cycleLength != null;
    }


    public CycleLength multiply( final CycleLength _operand ) {

        // sanity check...
        if( isNull( _operand ) ) throw new IllegalArgumentException( "operand may not be null" );

        // get the two operands in normal order...
        var a = this;
        var b = _operand;
        if( a.form.ordinal() > b.form.ordinal() ) {
            a = _operand;
            b = this;
        }

        // now do the right thing, depending on what form our operands are in...
        return switch( a.form ) {

            case AT_LEAST: yield switch( b.form ) {
                    case AT_LEAST, EXACTLY: yield new CycleLength( CycleLengthForm.AT_LEAST, a.cycleLength.multiply( b.cycleLength ) );
                    case INFINITE:          yield new CycleLength( CycleLengthForm.INFINITE, null );
                    case UNKNOWN:           yield new CycleLength( CycleLengthForm.AT_LEAST, a.cycleLength );
                };

            case EXACTLY: yield switch( b.form ) {
                    case EXACTLY:  yield new CycleLength( CycleLengthForm.EXACTLY, a.cycleLength.multiply( b.cycleLength ) );
                    case INFINITE: yield new CycleLength( CycleLengthForm.INFINITE, null );
                    case UNKNOWN:  yield new CycleLength( CycleLengthForm.AT_LEAST, a.cycleLength );
                    default:       throw new IllegalStateException( "Unexpected value: " + b.form );
            };

            case INFINITE: yield new CycleLength( CycleLengthForm.INFINITE, null );
            case UNKNOWN:  yield new CycleLength( CycleLengthForm.UNKNOWN,  null );
        };
    }
}
