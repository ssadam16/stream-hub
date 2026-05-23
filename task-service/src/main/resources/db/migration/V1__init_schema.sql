create table tasks (
                       id uuid primary key default gen_random_uuid(),
                       created_at timestamp not null default current_timestamp,
                       updated_at timestamp not null default current_timestamp,
                       title varchar(255) not null,
                       slug varchar(255) not null,
                       condition text not null,
                       author_id uuid not null,
                       difficulty varchar(32) not null,
                       status varchar(32) not null default 'DRAFT'
                           check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
                       external_id varchar(255) unique,
                       type varchar(32) not null
                           check (type in ('CONSOLE_IO', 'FUNCTION_UNIT', 'STRESS_TEST')),
                       points integer not null default 0 check (points >= 0),
                       time_limit_ms integer not null,
                       memory_limit_mb integer not null,
                       total_submissions bigint not null default 0,
                       successful_submissions bigint not null default 0,
                       constraint uk_tasks_slug unique (slug)
);

create index idx_tasks_status on tasks(status);
create index idx_tasks_difficulty on tasks(difficulty);

create table test_cases (
                            id uuid primary key default gen_random_uuid(),
                            created_at timestamp not null default current_timestamp,
                            updated_at timestamp not null default current_timestamp,
                            task_id uuid not null references tasks(id) on delete cascade,
                            input text not null,
                            expected_output text not null,
                            is_hidden boolean not null default false,
                            explanation text
);

create index idx_test_cases_task_id on test_cases(task_id);

create table tags (
                      id uuid primary key default gen_random_uuid(),
                      created_at timestamp not null default current_timestamp,
                      updated_at timestamp not null default current_timestamp,
                      name varchar(64) not null,
                      slug varchar(64) not null,
                      category varchar(32) not null
                          check (category in ('TOPIC', 'DATA_STRUCTURE', 'TECHNIQUE')),
                      constraint uk_tags_slug unique (slug),
                      constraint uk_tags_name unique (name)
);

create index idx_tags_category on tags(category);

create table task_tags (
                           task_id uuid not null references tasks(id) on delete cascade,
                           tag_id uuid not null references tags(id) on delete cascade,
                           primary key (task_id, tag_id)
);

create index idx_task_tags_tag_id on task_tags(tag_id);

-- seed tags
insert into tags (name, slug, category) values
                                            ('Math', 'math', 'TOPIC'),
                                            ('Bit Manipulation', 'bit-manipulation', 'TOPIC'),
                                            ('Geometry', 'geometry', 'TOPIC'),
                                            ('Combinatorics', 'combinatorics', 'TOPIC'),
                                            ('Number Theory', 'number-theory', 'TOPIC'),
                                            ('String', 'string', 'TOPIC'),
                                            ('Simulation', 'simulation', 'TOPIC'),
                                            ('Game Theory', 'game-theory', 'TOPIC'),
                                            ('Array', 'array', 'DATA_STRUCTURE'),
                                            ('Linked List', 'linked-list', 'DATA_STRUCTURE'),
                                            ('Stack', 'stack', 'DATA_STRUCTURE'),
                                            ('Queue', 'queue', 'DATA_STRUCTURE'),
                                            ('Hash Map', 'hash-map', 'DATA_STRUCTURE'),
                                            ('Hash Set', 'hash-set', 'DATA_STRUCTURE'),
                                            ('Tree', 'tree', 'DATA_STRUCTURE'),
                                            ('Binary Tree', 'binary-tree', 'DATA_STRUCTURE'),
                                            ('Binary Search Tree', 'binary-search-tree', 'DATA_STRUCTURE'),
                                            ('Heap', 'heap', 'DATA_STRUCTURE'),
                                            ('Graph', 'graph', 'DATA_STRUCTURE'),
                                            ('Trie', 'trie', 'DATA_STRUCTURE'),
                                            ('Union Find', 'union-find', 'DATA_STRUCTURE'),
                                            ('Segment Tree', 'segment-tree', 'DATA_STRUCTURE'),
                                            ('Fenwick Tree', 'fenwick-tree', 'DATA_STRUCTURE'),
                                            ('Matrix', 'matrix', 'DATA_STRUCTURE'),
                                            ('Two Pointers', 'two-pointers', 'TECHNIQUE'),
                                            ('Sliding Window', 'sliding-window', 'TECHNIQUE'),
                                            ('Binary Search', 'binary-search', 'TECHNIQUE'),
                                            ('Dynamic Programming', 'dynamic-programming', 'TECHNIQUE'),
                                            ('Greedy', 'greedy', 'TECHNIQUE'),
                                            ('Backtracking', 'backtracking', 'TECHNIQUE'),
                                            ('DFS', 'dfs', 'TECHNIQUE'),
                                            ('BFS', 'bfs', 'TECHNIQUE'),
                                            ('Recursion', 'recursion', 'TECHNIQUE'),
                                            ('Divide and Conquer', 'divide-and-conquer', 'TECHNIQUE'),
                                            ('Sorting', 'sorting', 'TECHNIQUE'),
                                            ('Prefix Sum', 'prefix-sum', 'TECHNIQUE'),
                                            ('Memoization', 'memoization', 'TECHNIQUE');