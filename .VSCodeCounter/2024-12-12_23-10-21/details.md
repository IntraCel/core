# Details

Date : 2024-12-12 23:10:21

Directory /home/jared/dev/projects/intracel/core

Total : 34 files,  1371 codes, 166 comments, 225 blanks, all 1762 lines

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [.clj-kondo/imports/taoensso/encore/config.edn](/.clj-kondo/imports/taoensso/encore/config.edn) | Clojure | 4 | 0 | 1 | 5 |
| [.clj-kondo/imports/taoensso/encore/taoensso/encore.clj](/.clj-kondo/imports/taoensso/encore/taoensso/encore.clj) | Clojure | 34 | 0 | 4 | 38 |
| [README.md](/README.md) | Markdown | 22 | 0 | 16 | 38 |
| [components/api/deps.edn](/components/api/deps.edn) | Clojure | 4 | 0 | 1 | 5 |
| [components/api/src/clj/intracel/api/interface/protocols.clj](/components/api/src/clj/intracel/api/interface/protocols.clj) | Clojure | 194 | 0 | 19 | 213 |
| [components/api/test/clj/intracel/api/interface_test.clj](/components/api/test/clj/intracel/api/interface_test.clj) | Clojure | 5 | 0 | 2 | 7 |
| [components/kv-store/.calva/output-window/output.calva-repl](/components/kv-store/.calva/output-window/output.calva-repl) | Clojure | 3 | 0 | 1 | 4 |
| [components/kv-store/deps.edn](/components/kv-store/deps.edn) | Clojure | 6 | 0 | 2 | 8 |
| [components/kv-store/src/clj/intracel/kv_store/interface.clj](/components/kv-store/src/clj/intracel/kv_store/interface.clj) | Clojure | 100 | 1 | 25 | 126 |
| [components/kv-store/src/clj/intracel/kv_store/lmdb.clj](/components/kv-store/src/clj/intracel/kv_store/lmdb.clj) | Clojure | 259 | 89 | 36 | 384 |
| [components/kv-store/test/clj/intracel/kv_store/interface_test.clj](/components/kv-store/test/clj/intracel/kv_store/interface_test.clj) | Clojure | 40 | 0 | 4 | 44 |
| [components/serde/deps.edn](/components/serde/deps.edn) | Clojure | 4 | 0 | 1 | 5 |
| [components/serde/src/clj/intracel/serde/big_decimal_serde.clj](/components/serde/src/clj/intracel/serde/big_decimal_serde.clj) | Clojure | 19 | 7 | 4 | 30 |
| [components/serde/src/clj/intracel/serde/big_int_helper.clj](/components/serde/src/clj/intracel/serde/big_int_helper.clj) | Clojure | 30 | 4 | 5 | 39 |
| [components/serde/src/clj/intracel/serde/big_int_serde.clj](/components/serde/src/clj/intracel/serde/big_int_serde.clj) | Clojure | 13 | 0 | 4 | 17 |
| [components/serde/src/clj/intracel/serde/byte_serde.clj](/components/serde/src/clj/intracel/serde/byte_serde.clj) | Clojure | 24 | 8 | 4 | 36 |
| [components/serde/src/clj/intracel/serde/double_serde.clj](/components/serde/src/clj/intracel/serde/double_serde.clj) | Clojure | 23 | 7 | 4 | 34 |
| [components/serde/src/clj/intracel/serde/float_serde.clj](/components/serde/src/clj/intracel/serde/float_serde.clj) | Clojure | 23 | 7 | 4 | 34 |
| [components/serde/src/clj/intracel/serde/int_serde.clj](/components/serde/src/clj/intracel/serde/int_serde.clj) | Clojure | 23 | 7 | 4 | 34 |
| [components/serde/src/clj/intracel/serde/interface.clj](/components/serde/src/clj/intracel/serde/interface.clj) | Clojure | 103 | 0 | 27 | 130 |
| [components/serde/src/clj/intracel/serde/long_serde.clj](/components/serde/src/clj/intracel/serde/long_serde.clj) | Clojure | 22 | 7 | 4 | 33 |
| [components/serde/src/clj/intracel/serde/nippy_serde.clj](/components/serde/src/clj/intracel/serde/nippy_serde.clj) | Clojure | 25 | 4 | 3 | 32 |
| [components/serde/src/clj/intracel/serde/short_serde.clj](/components/serde/src/clj/intracel/serde/short_serde.clj) | Clojure | 23 | 7 | 4 | 34 |
| [components/serde/src/clj/intracel/serde/string_serde.clj](/components/serde/src/clj/intracel/serde/string_serde.clj) | Clojure | 18 | 6 | 4 | 28 |
| [components/serde/src/clj/intracel/serde/uint_128_serde.clj](/components/serde/src/clj/intracel/serde/uint_128_serde.clj) | Clojure | 69 | 9 | 14 | 92 |
| [components/serde/test/clj/intracel/serde/interface_test.clj](/components/serde/test/clj/intracel/serde/interface_test.clj) | Clojure | 219 | 1 | 16 | 236 |
| [core.code-workspace](/core.code-workspace) | JSON with Comments | 26 | 0 | 0 | 26 |
| [deps.edn](/deps.edn) | Clojure | 14 | 0 | 5 | 19 |
| [docs/kv-store.md](/docs/kv-store.md) | Markdown | 2 | 0 | 0 | 2 |
| [docs/polylith.md](/docs/polylith.md) | Markdown | 8 | 0 | 6 | 14 |
| [get-clj-doc-container.sh](/get-clj-doc-container.sh) | Shell Script | 1 | 1 | 0 | 2 |
| [run-local-clj-doc.sh](/run-local-clj-doc.sh) | Shell Script | 1 | 1 | 0 | 2 |
| [set-jvm-opts.sh](/set-jvm-opts.sh) | Shell Script | 1 | 0 | 0 | 1 |
| [workspace.edn](/workspace.edn) | Clojure | 9 | 0 | 1 | 10 |

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)