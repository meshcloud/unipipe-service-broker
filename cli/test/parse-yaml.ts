// a KISS deno script to parse yaml and output it as json
// this allows us to test a) yaml is valid and b) access properties using jq

import * as yaml from 'https://deno.land/std@0.99.0/encoding/yaml.ts';
import * as util from 'https://deno.land/std@0.99.0/io/util.ts';

const stdin = new TextDecoder().decode(
  await util.readAll(Deno.stdin),
);

const obj = yaml.parse(stdin);

const quiet = Deno.args.includes("-q");

if (!quiet) {
  console.log(JSON.stringify(obj));
}
