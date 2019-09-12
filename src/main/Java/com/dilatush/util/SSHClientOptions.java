package com.dilatush.util;

/**
 * Enumerates all the SSH client options.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public enum SSHClientOptions {
    AddKeysToAgent, AddressFamily, BatchMode, BindAddress, CanonicalDomains, CanonicalizeFallbackLocal, CanonicalizeHostname, CanonicalizeMaxDots,
    CanonicalizePermittedCNAMEs, CertificateFile, ChallengeResponseAuthentication, CheckHostIP, Ciphers, ClearAllForwardings, Compression,
    ConnectionAttempts, ConnectTimeout, ControlMaster, ControlPath, ControlPersist, DynamicForward, EscapeChar, ExitOnForwardFailure, FingerprintHash,
    ForwardAgent, ForwardX11, ForwardX11Timeout, ForwardX11Trusted, GatewayPorts, GlobalKnownHostsFile, GSSAPIAuthentication,
    GSSAPIDelegateCredentials, HashKnownHosts, Host, HostbasedAuthentication, HostbasedKeyTypes, HostKeyAlgorithms, HostKeyAlias, HostName,
    IdentitiesOnly, IdentityAgent, IdentityFile, Include, IPQoS, KbdInteractiveAuthentication, KbdInteractiveDevices, KexAlgorithms, LocalCommand,
    LocalForward, LogLevel, MACs, Match, NoHostAuthenticationForLocalhost, NumberOfPasswordPrompts, PasswordAuthentication, PermitLocalCommand,
    PKCS11Provider, Port, PreferredAuthentications, ProxyCommand, ProxyJump, ProxyUseFdpass, PubkeyAcceptedKeyTypes, PubkeyAuthentication, RekeyLimit,
    RemoteCommand, RemoteForward, RequestTTY, SendEnv, ServerAliveInterval, ServerAliveCountMax, StreamLocalBindMask, StreamLocalBindUnlink,
    StrictHostKeyChecking, TCPKeepAlive, Tunnel, TunnelDevice, UpdateHostKeys, UsePrivilegedPort, User, UserKnownHostsFile, VerifyHostKeyDNS,
    VisualHostKey, XAuthLocation;
}
