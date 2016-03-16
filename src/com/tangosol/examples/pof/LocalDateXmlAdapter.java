package com.tangosol.examples.pof;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * An implementation of {@link XmlAdapter} because of lack of
 * support for DateTime in JAXB in Java 8.<p>
 * <p>
 * Refer: http://www.tagwith.com/question_134230_jax-rs-and-java-time-localdate-as-input-parameter
 *
 * @author tam  2015.07.10
 * @since 12.2.1
 */
public class LocalDateXmlAdapter
        extends XmlAdapter<String, LocalDate>
    {
    // ----- XmlAdapter methods ---------------------------------------------

    @Override
    public LocalDate unmarshal(String sDateString) throws Exception
        {
        return LocalDate.parse(sDateString, DateTimeFormatter.ISO_DATE);
        }

    @Override
    public String marshal(LocalDate ldLocalDate) throws Exception
        {
        return DateTimeFormatter.ISO_DATE.format(ldLocalDate);
        }
    }
