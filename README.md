# DeP10
Exploring extensions of Pep/10 to support OS and Compilers courses at DePauw

## Running

Given the following source in `test.pep`:

```
     @STRO  msg,d
     @CHARO 42,i
     @DECO  42,i
     @CHARO '\n',i
     RET
msg: .ASCII "Hello\n\0"
```

Assemble it with (second version also produces listing and error files):

```
java -jar dep10.jar asm test.pep -o test.pepo
java -jar dep10.jar asm test.pep -o test.pepo -l test.pepl -e test.pepe --os-listing
```

Run it with (second version also produces output, error, dump, and tracing files):

```
java -jar dep10.jar run test.pepo
java -jar dep10.jar run test.pepo -o test.out -e test.err -d test.pepd -t test.pept
```

