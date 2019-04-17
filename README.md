# Black Mango

[![Build Status](https://travis-ci.org/t-bast/black-mango.svg?branch=master)](https://travis-ci.org/t-bast/black-mango)
[![codecov](https://codecov.io/gh/t-bast/black-mango/branch/master/graph/badge.svg)](https://codecov.io/gh/t-bast/black-mango)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

An implementation of Boneh-Franklin's [Identity-Based Encryption](https://link.springer.com/content/pdf/10.1007/3-540-44647-8_13.pdf) scheme using [Akka](https://akka.io/).

This is an educational project, it is definitely not meant to be used in production.

## Building

This project is using [jPBC](http://gas.dia.unisa.it/projects/jpbc) for pairings and [tink](https://github.com/google/tink) for standard cryptography.

## Known issues

This is very likely completely insecure because the code is currently using a Type A pairing instead of a Type B pairing. This breaks the security of the mapping to curve point and probably breaks other parts of the scheme. A detailed security analysis should be done to see if the changes compared to the paper are harmful or not.

Another known issue is that this currently only implements the basic identity based encryption scheme, which is not chosen-ciphertext secure. We should use the Fujisaki-Okamoto transformation to make it CCA-secure as described in the paper (it doesn't add much complexity).
