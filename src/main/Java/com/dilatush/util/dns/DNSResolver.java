package com.dilatush.util.dns;

// TODO: implement iterative resolution...
// TODO: implement delayed shutdown of TCP connection
// TODO: implement logic to handle:
// TODO:   - TCP-only recursive
// TODO:   - normal UDP on truncation TCP iterative
// TODO:   - TCP-only iterative
// TODO: Other:
// TODO:   - create DNSResolver that:
// TODO:     - has one instance of DNSNIO (which should be renamed)
// TODO:     - has any number of DNSResolverConnection instances
// TODO:     - has an optional cache
// TODO:     - has various ways of deciding which DNSResolverConnection to use
// TODO: Move DNS Resolver into its own project

import com.dilatush.util.ExecutorService;
import com.dilatush.util.dns.agent.DNSNIO;

import java.io.IOException;

public class DNSResolver {

    private final ExecutorService executor;

    private final DNSNIO nio;


    private DNSResolver() throws IOException {

        nio = new DNSNIO();
        executor = new ExecutorService();
    }
}
