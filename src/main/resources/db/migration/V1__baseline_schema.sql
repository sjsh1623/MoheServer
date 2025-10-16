--
-- PostgreSQL database dump
--


-- Dumped from database version 15.14 (Debian 15.14-1.pgdg12+1)
-- Dumped by pg_dump version 15.14 (Debian 15.14-1.pgdg13+1)


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;



--
-- Name: activities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.activities (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    activity_type character varying(50) NOT NULL,
    place_id bigint,
    "timestamp" timestamp without time zone DEFAULT now()
);


--
-- Name: activities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.activities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: activities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.activities_id_seq OWNED BY public.activities.id;


--
-- Name: batch_job_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


--
-- Name: batch_job_execution_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


--
-- Name: batch_job_execution_job_execution_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_execution_job_execution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_execution_job_execution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_job_execution_job_execution_id_seq OWNED BY public.batch_job_execution.job_execution_id;


--
-- Name: batch_job_execution_params; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    parameter_name character varying(100) NOT NULL,
    parameter_type character varying(100) NOT NULL,
    parameter_value character varying(2500),
    identifying character(1) NOT NULL
);


--
-- Name: batch_job_execution_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_instance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);


--
-- Name: batch_job_instance_job_instance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_instance_job_instance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_job_instance_job_instance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_job_instance_job_instance_id_seq OWNED BY public.batch_job_instance.job_instance_id;


--
-- Name: batch_job_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


--
-- Name: batch_step_execution_context; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


--
-- Name: batch_step_execution_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution_step_execution_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_step_execution_step_execution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch_step_execution_step_execution_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.batch_step_execution_step_execution_id_seq OWNED BY public.batch_step_execution.step_execution_id;


--
-- Name: bookmarks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bookmarks (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    place_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: bookmarks_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bookmarks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bookmarks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bookmarks_id_seq OWNED BY public.bookmarks.id;


--
-- Name: distributed_job_lock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.distributed_job_lock (
    id bigint NOT NULL,
    job_name character varying(100) NOT NULL,
    chunk_id character varying(255) NOT NULL,
    worker_id character varying(100) NOT NULL,
    worker_hostname character varying(255),
    status character varying(20) DEFAULT 'LOCKED'::character varying NOT NULL,
    locked_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL,
    retry_count integer DEFAULT 0,
    max_retries integer DEFAULT 3,
    last_error text,
    metadata jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: distributed_job_lock_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.distributed_job_lock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: distributed_job_lock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.distributed_job_lock_id_seq OWNED BY public.distributed_job_lock.id;


--
-- Name: email_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.email_verifications (
    id bigint NOT NULL,
    code character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    issued_at timestamp(6) with time zone,
    success boolean NOT NULL,
    verified_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: email_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.email_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: email_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.email_verifications_id_seq OWNED BY public.email_verifications.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: keyword_catalog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.keyword_catalog (
    id bigint NOT NULL,
    category character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    keyword character varying(100) NOT NULL,
    mbti_weights jsonb,
    updated_at timestamp(6) without time zone
);


--
-- Name: keyword_catalog_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.keyword_catalog_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: keyword_catalog_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.keyword_catalog_id_seq OWNED BY public.keyword_catalog.id;


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    used boolean
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: place_business_hours; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_business_hours (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    day_of_week character varying(10),
    open time without time zone,
    close time without time zone,
    description text,
    is_operating boolean DEFAULT true,
    last_order_minutes integer
);


--
-- Name: place_business_hours_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_business_hours_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_business_hours_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_business_hours_id_seq OWNED BY public.place_business_hours.id;


--
-- Name: place_description_vectors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_description_vectors (
    id bigint NOT NULL,
    combined_attributes_text text,
    created_at timestamp(6) with time zone,
    description_vector text NOT NULL,
    extraction_prompt_hash character varying(255),
    extraction_source character varying(255) NOT NULL,
    model_name character varying(255) NOT NULL,
    model_version character varying(255),
    raw_description_text text NOT NULL,
    selected_keywords text NOT NULL,
    updated_at timestamp(6) with time zone,
    place_id bigint NOT NULL
);


--
-- Name: place_description_vectors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_description_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_description_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_description_vectors_id_seq OWNED BY public.place_description_vectors.id;


--
-- Name: place_descriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_descriptions (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    original_description text,
    ai_summary text,
    ollama_description text,
    search_query character varying(500),
    updated_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_descriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_descriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_descriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_descriptions_id_seq OWNED BY public.place_descriptions.id;


--
-- Name: place_external_raw; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_external_raw (
    id bigint NOT NULL,
    external_id character varying(255) NOT NULL,
    fetched_at timestamp(6) without time zone,
    payload jsonb NOT NULL,
    place_id bigint,
    source character varying(50) NOT NULL
);


--
-- Name: place_external_raw_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_external_raw_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_external_raw_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_external_raw_id_seq OWNED BY public.place_external_raw.id;


--
-- Name: place_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_images (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    url text NOT NULL,
    order_index integer,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_images_id_seq OWNED BY public.place_images.id;


--
-- Name: place_mbti_descriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_mbti_descriptions (
    id bigint NOT NULL,
    description text NOT NULL,
    mbti character varying(4) NOT NULL,
    model character varying(100),
    place_id bigint NOT NULL,
    prompt_hash character varying(64) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: place_mbti_descriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_mbti_descriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_mbti_descriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_mbti_descriptions_id_seq OWNED BY public.place_mbti_descriptions.id;


--
-- Name: place_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_reviews (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    review_text text,
    order_index integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: place_reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_reviews_id_seq OWNED BY public.place_reviews.id;


--
-- Name: place_similarity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_similarity (
    place_id1 bigint NOT NULL,
    place_id2 bigint NOT NULL,
    co_users integer,
    cosine_bin numeric(5,4),
    jaccard numeric(5,4),
    updated_at timestamp(6) without time zone
);


--
-- Name: place_similarity_topk; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_similarity_topk (
    neighbor_place_id bigint NOT NULL,
    place_id bigint NOT NULL,
    co_users integer,
    cosine_bin numeric(5,4),
    jaccard numeric(5,4),
    rank smallint NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: place_sns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.place_sns (
    id bigint NOT NULL,
    place_id bigint NOT NULL,
    platform character varying(50) NOT NULL,
    url text NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: place_sns_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.place_sns_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: place_sns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.place_sns_id_seq OWNED BY public.place_sns.id;


--
-- Name: places; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.places (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    latitude numeric(10,8),
    longitude numeric(11,8),
    road_address text,
    website_url text,
    rating numeric(38,2) DEFAULT 0.0,
    review_count integer DEFAULT 0,
    category character varying(100)[],
    keyword character varying(255)[],
    keyword_vector text,
    opening_hours jsonb,
    parking_available boolean DEFAULT false,
    pet_friendly boolean DEFAULT false,
    ready boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    crawler_found boolean
);


--
-- Name: places_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.places_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: places_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.places_id_seq OWNED BY public.places.id;


--
-- Name: preferences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preferences (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    pref_key character varying(255) NOT NULL,
    pref_value character varying(255),
    user_id bigint NOT NULL
);


--
-- Name: preferences_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.preferences_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: preferences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.preferences_id_seq OWNED BY public.preferences.id;


--
-- Name: prompts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prompts (
    id bigint NOT NULL,
    content character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    place_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: prompts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.prompts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: prompts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.prompts_id_seq OWNED BY public.prompts.id;


--
-- Name: recent_views; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recent_views (
    id bigint NOT NULL,
    viewed_at timestamp(6) with time zone,
    place_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: recent_views_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recent_views_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recent_views_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recent_views_id_seq OWNED BY public.recent_views.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    is_revoked boolean
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: temp_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.temp_users (
    id character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    otp character varying(10) NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    verified boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp(6) with time zone NOT NULL,
    nickname character varying(255),
    password_hash character varying(255),
    terms_agreed boolean,
    verification_code character varying(255) NOT NULL
);


--
-- Name: temp_users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.temp_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: temp_users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.temp_users_id_seq OWNED BY public.temp_users.id;


--
-- Name: terms_agreements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.terms_agreements (
    id bigint NOT NULL,
    agreed boolean NOT NULL,
    agreed_at timestamp(6) with time zone,
    terms_code character varying(255) NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: terms_agreements_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.terms_agreements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: terms_agreements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.terms_agreements_id_seq OWNED BY public.terms_agreements.id;


--
-- Name: user_preference_vectors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_preference_vectors (
    id bigint NOT NULL,
    combined_preferences_text text,
    created_at timestamp(6) with time zone,
    extraction_prompt_hash character varying(255),
    extraction_source character varying(255) NOT NULL,
    model_name character varying(255) NOT NULL,
    model_version character varying(255),
    preference_vector text NOT NULL,
    raw_profile_text text NOT NULL,
    selected_keywords text NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: user_preference_vectors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_preference_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_preference_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_preference_vectors_id_seq OWNED BY public.user_preference_vectors.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    nickname character varying(50) NOT NULL,
    mbti character varying(4),
    age_range character varying(20),
    transportation_method character varying(50),
    space_preferences text[],
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_onboarding_completed boolean,
    last_login_at timestamp(6) with time zone,
    password_hash character varying(255) NOT NULL,
    profile_image_url character varying(255),
    transportation character varying(255)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vector_similarities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vector_similarities (
    place_id bigint NOT NULL,
    user_id bigint NOT NULL,
    calculated_at timestamp(6) with time zone,
    common_keywords integer NOT NULL,
    cosine_similarity numeric(5,4) NOT NULL,
    euclidean_distance numeric(8,4) NOT NULL,
    jaccard_similarity numeric(5,4),
    keyword_overlap_ratio numeric(3,2),
    mbti_boost_factor numeric(3,2),
    place_vector_version bigint,
    user_vector_version bigint,
    weighted_similarity numeric(5,4) NOT NULL
);


--
-- Name: activities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.activities ALTER COLUMN id SET DEFAULT nextval('public.activities_id_seq'::regclass);


--
-- Name: batch_job_execution job_execution_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_job_execution ALTER COLUMN job_execution_id SET DEFAULT nextval('public.batch_job_execution_job_execution_id_seq'::regclass);


--
-- Name: batch_job_instance job_instance_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_job_instance ALTER COLUMN job_instance_id SET DEFAULT nextval('public.batch_job_instance_job_instance_id_seq'::regclass);


--
-- Name: batch_step_execution step_execution_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch_step_execution ALTER COLUMN step_execution_id SET DEFAULT nextval('public.batch_step_execution_step_execution_id_seq'::regclass);


--
-- Name: bookmarks id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookmarks ALTER COLUMN id SET DEFAULT nextval('public.bookmarks_id_seq'::regclass);


--
-- Name: distributed_job_lock id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributed_job_lock ALTER COLUMN id SET DEFAULT nextval('public.distributed_job_lock_id_seq'::regclass);


--
-- Name: email_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.email_verifications ALTER COLUMN id SET DEFAULT nextval('public.email_verifications_id_seq'::regclass);


--
-- Name: keyword_catalog id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.keyword_catalog ALTER COLUMN id SET DEFAULT nextval('public.keyword_catalog_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: place_business_hours id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_business_hours ALTER COLUMN id SET DEFAULT nextval('public.place_business_hours_id_seq'::regclass);


--
-- Name: place_description_vectors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_description_vectors ALTER COLUMN id SET DEFAULT nextval('public.place_description_vectors_id_seq'::regclass);


--
-- Name: place_descriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_descriptions ALTER COLUMN id SET DEFAULT nextval('public.place_descriptions_id_seq'::regclass);


--
-- Name: place_external_raw id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_external_raw ALTER COLUMN id SET DEFAULT nextval('public.place_external_raw_id_seq'::regclass);


--
-- Name: place_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_images ALTER COLUMN id SET DEFAULT nextval('public.place_images_id_seq'::regclass);


--
-- Name: place_mbti_descriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_mbti_descriptions ALTER COLUMN id SET DEFAULT nextval('public.place_mbti_descriptions_id_seq'::regclass);


--
-- Name: place_reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_reviews ALTER COLUMN id SET DEFAULT nextval('public.place_reviews_id_seq'::regclass);


--
-- Name: place_sns id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_sns ALTER COLUMN id SET DEFAULT nextval('public.place_sns_id_seq'::regclass);


--
-- Name: places id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.places ALTER COLUMN id SET DEFAULT nextval('public.places_id_seq'::regclass);


--
-- Name: preferences id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preferences ALTER COLUMN id SET DEFAULT nextval('public.preferences_id_seq'::regclass);


--
-- Name: prompts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prompts ALTER COLUMN id SET DEFAULT nextval('public.prompts_id_seq'::regclass);


--
-- Name: recent_views id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recent_views ALTER COLUMN id SET DEFAULT nextval('public.recent_views_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: temp_users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.temp_users ALTER COLUMN id SET DEFAULT nextval('public.temp_users_id_seq'::regclass);


--
-- Name: terms_agreements id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.terms_agreements ALTER COLUMN id SET DEFAULT nextval('public.terms_agreements_id_seq'::regclass);


--
-- Name: user_preference_vectors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_preference_vectors ALTER COLUMN id SET DEFAULT nextval('public.user_preference_vectors_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: distributed_job_lock distributed_job_lock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributed_job_lock
    ADD CONSTRAINT distributed_job_lock_pkey PRIMARY KEY (id);


--
-- Name: place_mbti_descriptions uk8vgu097vv0kxvy9fl0e45q38o; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_mbti_descriptions
    ADD CONSTRAINT uk8vgu097vv0kxvy9fl0e45q38o UNIQUE (place_id, mbti);


--
-- Name: bookmarks uk99pie2n6pocv6d5pwa485kk0e; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bookmarks
    ADD CONSTRAINT uk99pie2n6pocv6d5pwa485kk0e UNIQUE (user_id, place_id);


--
-- Name: place_similarity_topk ukdmatkp081ka94wdfr7ohi81kn; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_similarity_topk
    ADD CONSTRAINT ukdmatkp081ka94wdfr7ohi81kn UNIQUE (place_id, rank);


--
-- Name: place_external_raw ukhpyvhd2nee2o953gancnkp1c3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.place_external_raw
    ADD CONSTRAINT ukhpyvhd2nee2o953gancnkp1c3 UNIQUE (source, external_id);


--
-- Name: distributed_job_lock uq_job_chunk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.distributed_job_lock
    ADD CONSTRAINT uq_job_chunk UNIQUE (job_name, chunk_id);


--
-- Name: idx_job_lock_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_job_lock_expires ON public.distributed_job_lock USING btree (expires_at);


--
-- Name: idx_job_lock_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_job_lock_status ON public.distributed_job_lock USING btree (job_name, status, expires_at);


--
-- Name: idx_job_lock_worker; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_job_lock_worker ON public.distributed_job_lock USING btree (worker_id, status);


--
-- Name: idx_place_external_raw_fetched_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_external_raw_fetched_at ON public.place_external_raw USING btree (fetched_at);


--
-- Name: idx_place_external_raw_place_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_external_raw_place_id ON public.place_external_raw USING btree (place_id);


--
-- Name: idx_place_external_raw_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_external_raw_source ON public.place_external_raw USING btree (source);


--
-- Name: idx_place_similarity_cosine; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_cosine ON public.place_similarity USING btree (cosine_bin);


--
-- Name: idx_place_similarity_jaccard; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_jaccard ON public.place_similarity USING btree (jaccard);


--
-- Name: idx_place_similarity_place1; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_place1 ON public.place_similarity USING btree (place_id1);


--
-- Name: idx_place_similarity_place2; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_place2 ON public.place_similarity USING btree (place_id2);


--
-- Name: idx_place_similarity_topk_neighbor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_topk_neighbor ON public.place_similarity_topk USING btree (neighbor_place_id);


--
-- Name: idx_place_similarity_topk_place; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_place_similarity_topk_place ON public.place_similarity_topk USING btree (place_id, rank);


--
-- PostgreSQL database dump complete
--


